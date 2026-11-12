param([string]$MavenRepository = (Join-Path $HOME '.m2/repository'), [string]$Tests = '', [switch]$Package)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$containerName = 'gamehub-tests-' + [guid]::NewGuid().ToString('N')
$environmentNames = @('MYSQL_ROOT_PASSWORD', 'MYSQL_PASSWORD', 'MYSQL_PWD',
    'GAMEHUB_TEST_MYSQL_URL', 'GAMEHUB_TEST_MYSQL_PASSWORD')
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$started = $false
Push-Location $projectRoot
try {
    & docker info --format '{{.ServerVersion}}'
    if ($LASTEXITCODE -ne 0) { throw 'Start Docker Desktop before running isolated MySQL tests.' }
    $env:MYSQL_ROOT_PASSWORD = [guid]::NewGuid().ToString('N')
    $env:MYSQL_PASSWORD = [guid]::NewGuid().ToString('N')
    $databasePassword = $env:MYSQL_PASSWORD
    & docker run -d --name $containerName --label gamehub.audit=automated-tests `
        --tmpfs /var/lib/mysql:rw -p 127.0.0.1::3306 `
        -e MYSQL_ROOT_PASSWORD -e MYSQL_PASSWORD -e MYSQL_USER=gamehub_audit `
        -e MYSQL_DATABASE=gamehub_cart_tests mysql:8.4
    if ($LASTEXITCODE -ne 0) { throw 'Could not create a new isolated MySQL container.' }
    $started = $true
    $env:MYSQL_PWD = $env:MYSQL_ROOT_PASSWORD
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        & docker exec -e MYSQL_PWD $containerName mysqladmin ping -h 127.0.0.1 -u root --silent 2>$null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'The new temporary MySQL server did not become ready.' }
    & docker exec -e MYSQL_PWD $containerName mysql -u root -e "CREATE DATABASE gamehub_migration_tests; CREATE DATABASE gamehub_migration_invalid; CREATE DATABASE gamehub_migration_unmanaged; GRANT ALL ON gamehub_migration_tests.* TO 'gamehub_audit'@'%'; GRANT ALL ON gamehub_migration_invalid.* TO 'gamehub_audit'@'%'; GRANT ALL ON gamehub_migration_unmanaged.* TO 'gamehub_audit'@'%';"
    if ($LASTEXITCODE -ne 0) { throw 'Could not prepare disposable migration fixtures.' }
    $binding = (& docker port $containerName 3306/tcp).Trim()
    if ($binding -notmatch '^127\.0\.0\.1:[0-9]+$') { throw 'Unexpected database port binding.' }
    $env:GAMEHUB_TEST_MYSQL_URL = 'jdbc:mysql://' + $binding + '/gamehub_cart_tests?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true'
    $env:GAMEHUB_TEST_MYSQL_PASSWORD = $databasePassword
    $mavenArguments = @('-B', "-Dmaven.repo.local=$MavenRepository")
    if ($Tests) { $mavenArguments += "-Dtest=$Tests" }
    if ($Package) { $mavenArguments += 'verify' } else { $mavenArguments += 'test' }
    & .\mvnw.cmd @mavenArguments
    if ($LASTEXITCODE -ne 0) { throw 'The isolated MySQL test suite failed. See target/surefire-reports.' }
}
finally {
    if ($started) {
        $container = (& docker inspect $containerName | ConvertFrom-Json)[0]
        if ($container.Name -eq ('/' + $containerName) -and
            $container.Config.Labels.'gamehub.audit' -eq 'automated-tests' -and
            $container.HostConfig.Tmpfs.'/var/lib/mysql') {
            & docker stop $containerName
            Write-Output 'Stopped the newly created test container; no container or file was removed.'
        } else {
            Write-Warning 'Container identity could not be verified; it was left untouched.'
        }
    }
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    Pop-Location
}
