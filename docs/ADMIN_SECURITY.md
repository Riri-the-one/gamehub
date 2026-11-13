> Historical batch report. Results, setup and limitations below describe an earlier stage and may be superseded. See the [current final report](FINAL_REPORT.md), [README](../README.md) and [migration policy](MIGRATIONS.md).

# Administrator security

Implemented on `fix/admin-security`, without changing entities, table mappings, or existing database contents.

## Local account setup

The administrator account is independent of database user records. Spring Security loads one account with role `ADMIN` from these environment variables:

- `GAMEHUB_ADMIN_EMAIL`: the exact email used to sign in.
- `GAMEHUB_ADMIN_PASSWORD_HASH`: a raw BCrypt hash, beginning with `$2a$`, `$2b$`, or `$2y$`; do not prepend `{bcrypt}`.

If either value is missing or the hash format is invalid, no administrator account is created. There is no default password and no database administrator fallback. Restart the application after changing these variables. Do not commit passwords, hashes, environment files, or terminal transcripts.

Generate a hash locally in an interactive PowerShell terminal from the repository root:

```powershell
.\mvnw.cmd dependency:build-classpath '-Dmdep.outputFile=target/admin-tool-classpath.txt' '-Dmdep.includeScope=runtime'
$adminToolClasspath = (Get-Content -Raw target/admin-tool-classpath.txt).Trim()
java --class-path $adminToolClasspath tools/HashAdminPassword.java
```

The utility prompts twice with hidden input, uses BCrypt cost 12, and prints only the hash. It requires at least 12 characters and at most 72 UTF-8 bytes. It refuses noninteractive input. No password is passed in a shell argument. Store the resulting hash locally in a password manager.

Set the current shell environment without placing a real hash in a script:

```powershell
$env:GAMEHUB_ADMIN_EMAIL = Read-Host 'Administrator email'
$env:GAMEHUB_ADMIN_PASSWORD_HASH = Read-Host 'Paste the locally generated BCrypt hash'
.\mvnw.cmd spring-boot:run
```

Configure a disposable development MySQL database as explained in the README before starting. Open `/admin/login` and enter the email and original password. `.env` files are not automatically loaded. A real deployed environment also needs HTTPS and an appropriate secret-management mechanism.

## Access rules and compatibility

- Public home, catalogue, static CSS/images and existing customer routes remain accessible. Unknown routes are denied by default.
- `/admin` and `/admin/**` require `ROLE_ADMIN`, except the login page/processing URL.
- Legacy entity CRUD routes and entity lists also require `ROLE_ADMIN`. Legacy save/update/delete handlers accept POST only; their missing templates remain a pre-existing limitation.
- Product/category deletion and logout now use POST forms. Styling and confirmation prompts are retained. Manual GET links to deletion endpoints no longer mutate data.
- Spring Security handles authentication, session fixation protection, authorization, and logout. An old `isAdmin` session flag grants no access.
- CSRF protection remains enabled globally. Thymeleaf `th:action` automatically inserts the hidden token in POST forms. The payment form received only this action-attribute adaptation; payment logic is unchanged.
- Batch 2 subsequently replaced cart removal/emptying with CSRF-protected POST forms and added guest ownership checks; see [cart security](CART_SECURITY.md). Order/payment ownership remains outside both batches.

## Tests

```powershell
.\mvnw.cmd test
```

Tests use test-scoped H2 in memory, with explicit test datasource configuration; no MySQL server is needed. `AdminSecurityTests` generates a random test password in memory, encodes it, and tests actual login plus role-based requests. Additional tests cover missing credentials and the original context smoke test.

Coverage includes anonymous access, non-admin role rejection, forged legacy session flags, administrator pages and product/category writes, absent/invalid CSRF tokens, POST-only deletions, login/logout, public rendering with a real test product, and disabled default access. These tests do not establish MySQL compatibility or complete checkout/payment correctness. The later [functional validation](FUNCTIONAL_VALIDATION.md) adds real MySQL/browser checks and screenshots; no historical pixel comparison is claimed.

Validation on 2026-11-12: **37 tests passed, 0 failures, 0 errors, 0 skipped**, using Maven 3.9.15, JDK 21.0.10 and Java 17 compilation target. Six existing Lombok builder-default warnings remain. The standalone hashing utility also compiled successfully; no real administrator password was generated. The initial test run had five incorrect absolute-redirect assertions, corrected to Spring Security's relative login URL before the successful runs.

## Changed files for batch 1

- `pom.xml`: Spring Security, security test helpers, and test-scoped H2.
- `src/main/java/com/example/miniecommerce/config/SecurityConfig.java`: account provisioning and filter chain.
- `controllers/UtilisateurController.java`, `services/UtilisateurService.java`, `services/impl/UtilisateurServiceImpl.java`: delegate administrator authentication/authorization to Spring Security; remove the hardcoded-password implementation.
- All eight controller files: legacy save/update/delete mappings restricted to POST. Customer cart, checkout, and payment business methods are unchanged.
- `templates/admin_login.html` and `templates/admin_dashboard.html`: CSRF-aware login/logout/deletion forms, retained visual classes and confirmation prompts.
- `templates/paiement.html`: only `action` changed to `th:action` so Thymeleaf adds CSRF; no payment behavior change.
- `src/test/java/com/example/miniecommerce/AdminSecurityTests.java`, `DisabledAdminSecurityTests.java`, `config/SecurityConfigTests.java`, and the existing `MiniEcommerceApplicationTests.java`: security and isolated context checks.
- `src/test/resources/application-test.properties`: disposable H2 defaults; context tests explicitly override datasource settings to prevent inherited personal database settings.
- `tools/HashAdminPassword.java`: interactive local hashing utility.
- `README.md`, `docs/TECHNICAL_REVIEW.md`, and this document: setup, verification and current limitations.

The previously pending `.gitignore` and `mvnw.cmd` changes were preserved. No entities, JPA mappings, sensitive application settings, or database migrations changed. No file deletion, commit, or push was performed.
