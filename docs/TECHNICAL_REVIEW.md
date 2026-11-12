> Historical batch report. Results, setup and limitations below describe an earlier stage and may be superseded. See the [current final report](FINAL_REPORT.md), [README](../README.md) and [migration policy](MIGRATIONS.md).

# GameHub technical review

> Subsequent step 3A implements owner-checked orders, atomic checkout and guarded simulated payments without migration. Current results are 77 passing MySQL tests and 22 full-purchase Edge checks; see [order security](ORDER_SECURITY.md). Older checkout/payment findings below describe the earlier baseline.

> Current status after security batches 1 and 2: administrator authentication and route authorization now use Spring Security with an environment-configured BCrypt account. CSRF is enabled globally and administrative writes require POST. Tests now use committed H2 isolation. See [administrator security](ADMIN_SECURITY.md) and the later [MySQL/browser validation with screenshots](FUNCTIONAL_VALIDATION.md). The audit and proposed plan below describe the earlier baseline; findings about hardcoded login, missing route protection, and absence of an isolated test profile are superseded by this implementation. Guest cart isolation, cart ownership, quantity validation and serialized guest writes are now implemented; see [cart security and its MySQL/Edge verification](CART_SECURITY.md). The shared-client/cart findings below are historical. Checkout/payment ownership and consistency, and missing legacy views, remain unresolved.

Review date: 2026-11-12. Scope: source, Maven configuration, templates, existing tests, and Git-tracked files in the local checkout. Findings are based on source inspection unless a command result is explicitly reported. Business code and datasource settings were not modified. Only new disposable in-memory databases were used during runtime checks.

## Source root and architecture

The Git root and Maven source root are the top-level `GameHub-spring-boot` directory containing `pom.xml` and `src/`. The similarly named nested directory contains only generated `target` directories in this checkout. Both root and nested build output are already ignored. Neither directory was deleted.

The application is a single-module Spring MVC monolith with eight controllers, eight service interfaces and implementations, eight JPA repositories, and eight entities. Thymeleaf provides twelve HTML templates; shared CSS is in `static/css/style.css`. Java 17 and Spring Boot 4.0.6 are declared in the POM; the wrapper selects Maven 3.9.15. No frontend package manager, API specification, migrations, database seed, Docker configuration, or CI workflow was found.

## Findings to prioritize

| Priority | Evidence | Implication and proposed follow-up |
| --- | --- | --- |
| Critical | `UtilisateurServiceImpl.authenticateAdmin` compares against a hardcoded password; `isAdmin` checks only whether a session attribute exists. | Replace the demonstration login with Spring Security, password hashing, and explicit role checks. |
| Critical | Legacy CRUD routes in the controllers lack the checks used by `/admin/*`; deletion routes also accept GET or unrestricted HTTP methods. | Centralize authorization and restrict writes to appropriate methods with CSRF protection. Missing views do not prevent preceding service calls from executing. |
| Critical | `getOrCreateDefaultClient` selects the first client record and `checkAndGetCurrentUser` reuses it across new sessions. | Separate guest carts and authenticated customers; current sessions can share cart and customer data. |
| Critical | Cart-line deletion, payment, and confirmation lookups accept record IDs without checking ownership. | Enforce access checks against the current customer before reading or changing records. |
| Critical | `CommandesServiceImpl.passerCommande` saves the order before checking every product's stock and has no service-level transaction. | A later failure can leave a partial order or stock changes. Add atomic checkout and concurrency handling with rollback tests. |
| Critical | `validerPaiementCommande` confirms an order before parsing the payment method and creates a payment on each invocation. | Validate before writes, use a transaction, and make repeated payment submission safe. Payment remains a simulation. |
| Important | `LignePaniersServiceImpl.addProduitToPanier` accepts arbitrary quantities; controller model binding uses entities directly. | Add server-side validation, request DTOs, and explicit missing-record handling. Test zero/negative quantities, nulls, and invalid IDs. |
| Important | Prices and totals use `Double`; enums lack explicit `@Enumerated` mapping. | Plan decimal money types and stable enum storage through a separately reviewed schema migration. No mapping changes were made. |
| Important | Services repeatedly load `findAll()` and filter in Java; product search overrides category filtering. | Use repository queries and pagination; clarify intended combined-filter semantics before changing behavior. |
| Important | Several entities use Lombok `@Data` on bidirectional associations; builder collection defaults lack `@Builder.Default`. | Review generated equality/string methods for recursion and lazy loading, and test collection initialization. |
| Important | The tracked datasource configuration enables automatic database creation and `ddl-auto=update`. | Introduce explicit local/test profiles and managed migrations later. The context test currently inherits database-mutating configuration. |

## Technical and documentation inconsistencies

Severity definitions: **Critical** means unauthorized access, shared personal data, or inconsistent order/stock/payment writes; **Important** means broken behavior, missing validation, reproducibility gaps, or maintainability issues with runtime consequences; **Minor** means naming, presentation, or incomplete project metadata. The previous High/Medium labels have been replaced accordingly. Missing legacy views and the absence of isolated MySQL tests are Important. Package branding, repository spelling, empty POM metadata, and missing screenshots are Minor. The simulated payment is a declared prototype limitation rather than an undisclosed provider integration.

- The Maven artifact and application name are GameHub, while the package and main class remain `com.example.miniecommerce.MiniEcommerceApplication`. This is functional but inconsistent branding. Repository names also use the nonstandard spelling `Repositorie`. Renaming is a separate refactor.
- Controllers still return legacy views such as `CreateCategories`, `CategoriesList`, `EditCategories`, `CreateProduits`, and analogous CRUD views for other entities. These templates are absent. The existing lowercase storefront and `admin_*` templates cover a different set of routes. Add MVC view-resolution tests before deciding whether to restore or remove legacy routes.
- Administrator and payment pages exist, but neither secure production authentication nor a real payment provider is implemented. Documentation must distinguish these flows from production integrations.
- No data seed or administrator bootstrap exists. Fresh installation instructions cannot promise a populated catalogue or ready-to-use admin account.
- The POM contains empty URL, license, developer, and SCM metadata elements. There is no license file. Populate verified project metadata and choose a license separately; do not invent author details or legal terms.
- No README existed before this review. The new README documents the actual root, setup requirements, feature boundaries, screenshots status, and verification limits.

## Recommended tests

The only existing test is `MiniEcommerceApplicationTests.contextLoads()`. It is a full context smoke test with no isolated datasource configuration. No business assertions, MVC tests, or coverage configuration were found. The following tests are recommendations, not newly implemented tests:

1. **Service unit tests with JUnit and Mockito, no Spring context or database:** product-name search, category filter and search precedence; cart total and empty cart; adding an existing product merges quantities; missing products/carts; order-line price snapshots and stock decrease on a successful checkout.
2. **Regression tests paired with fixes:** nonpositive quantities, shared-client isolation, unauthorized cart/order access, insufficient stock partway through checkout, invalid payment methods, and repeated payment submissions. Assertions should describe the intended safe behavior; several will expose current defects.
3. **MVC tests:** real template rendering for storefront/admin pages, login redirects, invalid input handling, and detection of every missing legacy view. Include authorization, CSRF, and allowed HTTP-method tests when security is introduced.
4. **MySQL integration tests using a disposable database, preferably Testcontainers:** JPA relationship persistence, foreign-key behavior, order rollback, stock concurrency, and payment uniqueness. Add a test profile first and never use personal or production data. H2 alone would not establish MySQL compatibility.
5. **Build automation after reproducible local validation:** run `verify` in CI with the declared Java target and isolated database, publish test reports, and introduce JaCoCo only with useful behavioral tests. Do not add a coverage badge before measurement exists.

## Repository hygiene

- `git status --short` was clean before edits.
- `git ls-files` showed source, templates, build descriptors, and Maven wrapper scripts/configuration. No tracked compiled classes, archives, database dumps, logs, or image files were found. `git ls-files -ci --exclude-standard` returned no tracked files matching ignore rules.
- `.idea/` and generated `target/` content are ignored. The wrapper uses `distributionType=only-script`, so the absence of a tracked wrapper JAR is consistent with its configuration.
- `.gitignore` now also excludes environment files (while allowing `.env.example`), logs, root-level local Spring configuration overrides, and common operating-system metadata. These rules do not untrack existing files or protect Git history.
- `src/main/resources/application.properties` remains tracked and unchanged. It contains datasource credentials/settings; values are not reproduced here. If any value has ever been a real credential, review exposure and rotation before publishing. This review did not audit historical commits or remote hosting settings.
- No files were removed or untracked. No commit or push was performed.

## Verification performed

| Check | Result |
| --- | --- |
| Git root, POM, source tree, nested directory | Inspected; top-level directory is the only source module found. |
| Controllers, services, repositories, entities, enums, templates and CSS | Inspected for architecture, features, routes, persistence and documentation claims. |
| Image assets | No standalone screenshots or image files found; README contains no invented images. |
| Java runtime | Local `java -version` reports JDK 21.0.10; POM target remains Java 17. |
| Maven availability | No standalone `mvn` command found on PATH. |
| Wrapper version command | Initial `NullArray` reproduced; after correction, Maven 3.9.15 started successfully. |
| Test attempt | Initial wrapper failure, then incomplete offline Surefire cache; online dependency resolution succeeded. Existing context test passed on temporary H2: 1 run, 0 failures/errors/skipped. |

The original wrapper indexed `(Get-Item $MAVEN_M2_PATH).Target[0]`. The `.m2` directory has a null `Target` on Windows PowerShell 5.1.26100.9818; indexing it reproduces the exact error independently. The wrapper now checks the target before indexing and uses `-LiteralPath`. Maven 3.9.15 startup was then verified. Dependency versions and wrapper properties are unchanged.

Compilation and the existing context smoke test have now passed as detailed below. Packaging, MySQL integration, complete purchase flows, and visual browser inspection remain unverified. Changes delivered are documentation, ignore rules, and the Windows wrapper null-target correction.

## Follow-up verification and environment diagnosis

- `JAVA_HOME` points to a valid JDK 21.0.10; both `java` and `javac` report that version. Compilation used `release 17`; execution on a Java 17 runtime was not tested.
- No `mvn` is on PATH. `MAVEN_HOME` points to a nonexistent Maven 3.9.12 directory. `MAVEN_USER_HOME` and `M2_HOME` are unset. No persistent environment variable was changed. The wrapper selects its own Maven 3.9.15 distribution independently of `MAVEN_HOME`.
- The corrected wrapper first encountered a sandbox permission failure creating its user-cache distribution directory. After approval it downloaded Maven and returned its version successfully. This was separate from the fixed null-indexing defect.
- In the restricted shell Maven initially tried inaccessible `C:\.m2\repository`. Passing `-Dmaven.repo.local` with the existing user repository explicitly and granting cache access resolved that environmental restriction.
- Original-POM compilation command: `.\mvnw.cmd -o "-Dmaven.repo.local=$HOME/.m2/repository" -DskipTests compile`. Result: **BUILD SUCCESS**, 45 Java files compiled, six Lombok collection-default warnings. This command did not run tests or connect to a datasource.
- The temporary `target/portfolio-audit/pom.xml` is derived from the original POM, adds only test-scoped H2, and points at the original source and resource directories. It leaves `pom.xml` and all source files unchanged. Its build output is separate and ignored by Git. It is a local audit artifact, not a committed test profile.
- The first offline test attempt failed because Surefire 3.5.5 had an incomplete cache (missing POM and `SurefireReportParameters`). Retrying with approved dependency downloads resolved this without deleting cache files or changing plugin versions.
- Successful test settings: `spring.datasource.url=jdbc:h2:mem:gamehub_portfolio_audit;MODE=MySQL;DB_CLOSE_DELAY=-1`, driver `org.h2.Driver`, username `sa`, empty password, `ddl-auto=create-drop`. Schema creation/drop concerned only this new memory database. Surefire XML reports **tests=1, failures=0, errors=0, skipped=0**. H2 2.4.240 is test-only in the temporary POM; it is not part of the project's declared stack.
- Reports remain locally under `target/portfolio-audit/target/surefire-reports/`. They are ignored and should remain unpublished because reports can include environment metadata. JPA open-in-view and dynamic Mockito/Byte Buddy agent warnings were observed; neither caused test failure.
- Docker CLI exists, but its engine pipe is unavailable; no MySQL container was started. MySQL integration remains unverified.
- Using the successful test classpath, a temporary JVM ran the application on `127.0.0.1:18087` with a different H2 memory database, `gamehub_preview`, and explicit datasource overrides. HTTP requests in one session returned 200 for `/`, `/produits`, `/panier`, `/checkout`, and `/admin/login`. `/admin` followed a redirect to `/admin/login`. Only empty-data rendering was checked; no admin authentication, payment, or checkout submission was exercised.
- The preview JVM was stopped. No screenshot was taken. Captures are feasible using a similarly isolated local instance, but a populated demonstration and visual inspection are still needed.
- Final Git review covers `.gitignore`, `mvnw.cmd`, `README.md`, and this review. No source, POM, or application-properties diff exists. The tracked datasource password is empty; the hardcoded administrator credential remains a security defect in existing source. No generated artifacts or new confidential configuration appear among the pending changes. This does not certify all Git history or external account settings.

## Original implementation plan (batch 1 subsequently authorized)

Batches 1 and the cart portion of batch 2 were subsequently authorized and implemented on fix/admin-security. Guest carts now have isolated session identities, ownership checks, POST/CSRF protections and concurrent-write tests. Order/payment ownership, checkout consistency and subsequent batches remain pending. See CART_SECURITY.md for current results.

1. **Secure administrative access and writes:** introduce centralized Spring Security rules, restrict write methods, enable CSRF protection, and close the legacy CRUD bypasses. Replace the hardcoded password with a hashed credential loaded through a local setup procedure. Decide how existing administrator accounts are provisioned before implementation; any schema change needs a separate migration proposal and authorization.
2. **Isolate customer carts and records:** create a distinct guest identity/cart per session instead of selecting the first client. Enforce ownership for cart-line deletion, orders, payments, and confirmation pages. Test two independent sessions and cross-user IDs. Define an expiry strategy for guest records; no automatic deletion is authorized.
3. **Make checkout consistent:** validate quantities and all stock before writes, apply service transactions, handle concurrent purchases, and validate payment methods before state changes. Make payment confirmation idempotent. Add rollback and repeat-submission regression tests. Propose locking/uniqueness migrations separately if needed; do not silently change the schema.
4. **Establish reproducible verification:** add a committed isolated test setup, service and MVC regression tests, and disposable MySQL integration tests. Add CI only after local commands are reliable. Then review missing views and validation, with removal of legacy files/routes submitted separately.
5. **Polish presentation:** resolve naming and metadata, prepare non-sensitive demonstration data and real screenshots, and review the README again against the corrected behavior.

Each implementation batch should be reviewable separately. This plan does not authorize database migrations, deleting files/data, commits, or pushes. The repository is not yet declared ready for publication.
