param([switch]$Screenshots, [string]$EdgePath = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe')

$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$auditDirectory=Join-Path $projectRoot 'target/browser-audit'
$containerName='gamehub-demo-'+[guid]::NewGuid().ToString('N')
$names=@('MYSQL_ROOT_PASSWORD','MYSQL_PASSWORD','MYSQL_PWD','GAMEHUB_DEMO_PASSWORD','GAMEHUB_ADMIN_EMAIL',
    'GAMEHUB_ADMIN_PASSWORD_HASH','SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD',
    'SPRING_JPA_HIBERNATE_DDL_AUTO','SPRING_FLYWAY_ENABLED','SPRING_FLYWAY_BASELINE_ON_MIGRATE',
    'SERVER_SERVLET_SESSION_COOKIE_SECURE','NODE_PATH','GAMEHUB_BROWSER_STATE','GAMEHUB_CAPTURE_SCREENSHOTS','GAMEHUB_EDGE_PATH')
$previous=@{};foreach($name in $names){$previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
$started=$false;$application=$null
Push-Location $projectRoot
try {
    if(-not (Test-Path -LiteralPath $EdgePath)){throw 'Microsoft Edge is required; provide -EdgePath if installed elsewhere.'}
    $probe=New-Object Net.Sockets.TcpClient
    try {$probe.Connect('127.0.0.1',18087);$occupied=$true} catch {$occupied=$false} finally {$probe.Dispose()}
    if($occupied){throw 'Port 18087 is already in use; no process was stopped.'}
    New-Item -ItemType Directory -Path $auditDirectory -Force | Out-Null
    & .\mvnw.cmd -B "-Dmaven.repo.local=$HOME/.m2/repository" -DskipTests package dependency:build-classpath '-Dmdep.outputFile=target/browser-audit/classpath.txt' '-Dmdep.includeScope=runtime'
    if($LASTEXITCODE -ne 0){throw 'Could not build the executable application.'}
    & npm.cmd install --prefix target/browser-tools --no-save --no-audit --no-fund playwright-core@1.65.0
    if($LASTEXITCODE -ne 0){throw 'Could not prepare the browser driver.'}
    $state=@{container=$containerName;rootPassword=[guid]::NewGuid().ToString('N');dbPassword=[guid]::NewGuid().ToString('N');
        adminPassword=[guid]::NewGuid().ToString('N');adminEmail='portfolio@example.invalid'}
    $env:MYSQL_ROOT_PASSWORD=$state.rootPassword;$env:MYSQL_PASSWORD=$state.dbPassword
    & docker run -d --name $containerName --label gamehub.audit=browser-demo --tmpfs /var/lib/mysql:rw -p 127.0.0.1::3306 `
        -e MYSQL_ROOT_PASSWORD -e MYSQL_PASSWORD -e MYSQL_USER=gamehub_audit -e MYSQL_DATABASE=gamehub_portfolio mysql:8.4
    if($LASTEXITCODE -ne 0){throw 'Could not create a fresh demo database.'};$started=$true
    $env:MYSQL_PWD=$state.rootPassword;$ready=$false
    for($attempt=0;$attempt -lt 60;$attempt++) {
        & docker exec -e MYSQL_PWD $containerName mysqladmin ping -h 127.0.0.1 -u root --silent 2>$null
        if($LASTEXITCODE -eq 0){$ready=$true;break};Start-Sleep -Seconds 1
    }
    if(-not $ready){throw 'Demo database did not become ready.'}
    $binding=(& docker port $containerName 3306/tcp).Trim()
    if($binding -notmatch '^127\.0\.0\.1:[0-9]+$'){throw 'Unexpected database port binding.'}
    $env:GAMEHUB_DEMO_PASSWORD=$state.adminPassword
    $classpath=(Get-Content -Raw (Join-Path $auditDirectory 'classpath.txt')).Trim()
    $hash=& "$env:JAVA_HOME/bin/java.exe" --class-path $classpath tools/EncodeDemoPassword.java
    if($LASTEXITCODE -ne 0){throw 'Could not hash the disposable admin password.'}
    $env:GAMEHUB_ADMIN_EMAIL=$state.adminEmail;$env:GAMEHUB_ADMIN_PASSWORD_HASH=$hash
    $env:SPRING_DATASOURCE_URL='jdbc:mysql://'+$binding+'/gamehub_portfolio?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true'
    $env:SPRING_DATASOURCE_USERNAME='gamehub_audit';$env:SPRING_DATASOURCE_PASSWORD=$state.dbPassword
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO='validate';$env:SPRING_FLYWAY_ENABLED='true';$env:SPRING_FLYWAY_BASELINE_ON_MIGRATE='false'
    $env:SERVER_SERVLET_SESSION_COOKIE_SECURE='false'
    $statePath=Join-Path $auditDirectory 'state.json'
    [IO.File]::WriteAllText($statePath,($state|ConvertTo-Json),(New-Object System.Text.UTF8Encoding($false)))
    $application=Start-Process -FilePath "$env:JAVA_HOME/bin/java.exe" -ArgumentList @('-jar',('"'+(Join-Path $projectRoot 'target/GameHub-0.0.1-SNAPSHOT.jar')+'"'),'--server.address=127.0.0.1','--server.port=18087') `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $auditDirectory 'application.log') -RedirectStandardError (Join-Path $auditDirectory 'application-errors.log')
    $ready=$false
    for($attempt=0;$attempt -lt 60;$attempt++) {
        if($application.HasExited){throw 'Application startup failed; inspect target/browser-audit logs.'}
        try {$response=Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:18087/';if($response.StatusCode -eq 200){$ready=$true;break}} catch { }
        Start-Sleep -Seconds 1
    }
    if(-not $ready){throw 'Application did not become ready.'}
    $env:NODE_PATH=Join-Path $projectRoot 'target/browser-tools/node_modules';$env:GAMEHUB_BROWSER_STATE=$statePath
    $env:GAMEHUB_EDGE_PATH=$EdgePath;$env:GAMEHUB_CAPTURE_SCREENSHOTS=$Screenshots.IsPresent.ToString().ToLowerInvariant()
    & node tools/browser-checks.cjs
    if($LASTEXITCODE -ne 0){throw 'Browser checks failed; no passing result is claimed.'}
}
finally {
    if($application -and -not $application.HasExited){Stop-Process -Id $application.Id}
    if($started) {
        $container=(& docker inspect $containerName|ConvertFrom-Json)[0]
        if($container.Name -eq ('/'+$containerName) -and $container.Config.Labels.'gamehub.audit' -eq 'browser-demo' -and $container.HostConfig.Tmpfs.'/var/lib/mysql') {
            & docker stop $containerName
        }else{Write-Warning 'Container identity mismatch; left untouched.'}
    }
    foreach($name in $names){[Environment]::SetEnvironmentVariable($name,$previous[$name],'Process')}
    Pop-Location
}
