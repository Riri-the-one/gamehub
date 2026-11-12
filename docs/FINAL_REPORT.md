# Final engineering and validation report

Date: 2026-11-12. Branch: `fix/admin-security`. This report records pre-publication validation and supersedes the historical batch reports. No commit, push or publication had been performed at the time of this validation.

## Outcome

The application compiles and packages successfully. **98 automated tests passed: zero failures, errors or skipped tests.** The final real Microsoft Edge run passed **49 checks**, including **22 responsive page/viewport checks**, and produced **11 current screenshots**. All runtime verification used newly created, isolated MySQL 8.4.13 Docker databases with fictitious data. No existing database was accessed, migrated or deleted.

The repository is suitable for presentation as a tested student portfolio prototype. It is not a claim of production readiness or an independent security certification.

## Changes and corrected risks

| Area | Correction |
|---|---|
| Money | Replaced floating-point entity/service calculations with BigDecimal / DECIMAL(19,2), two decimals and HALF_UP rounding. Reject invalid/negative/excess-precision new prices and out-of-range totals. |
| Durable replay | Persistent unique hashed checkout key, payload fingerprint and owner verification. Same-key retries survive HTTP session loss while valid guest proof remains. Different payload conflicts; foreign owner receives 404. |
| Guest recovery | Random seven-day HttpOnly SameSite=Lax cookie, hashed proof and server-side expiry; explicit logout rotates/revokes proof. Historical shared customers are never adopted. |
| Inventory | Product versioning, locked/refreshed admin edits, stale-edit conflicts, ordered product locking and conditional stock deduction. Prevent deletion of referenced products/categories. |
| Atomicity | Checkout, order lines, stock and cart changes are transactional. The rollback test fails after order/line/stock writes have flushed and checks complete rollback. |
| Payment states | Validated simulated methods, one payment per order, conditional state transitions, same-method replay and owner checks. Concurrent cancellation/payment cannot produce a paid cancelled order. |
| Abandonment | Owned pending cancellation restores stock once; optional bounded expiry worker excludes historical orders and is disabled by default. |
| Legacy routes | Unsupported generic CRUD routes denied, including authenticated administrators, to prevent bypassing financial invariants. Supported admin product/category flows remain available. |
| Validation | Prices, product fields, quantity bounds, contact presence/length/email, server-calculated totals and image filename restrictions. |
| Presentation | Shared responsive CSS, mobile stacked table cards, wrapped navigation, accessible button/input sizing, themed error page and truthful confirmation states. |
| Quality | Corrected Lombok builder defaults; association exclusions avoid recursive entity string/equality expansion. Removed obsolete H2 dependency and unused imports. |
| Repository | Safe datasource defaults, Flyway validation, isolated reproducible scripts, CI configuration, current English README, real screenshot gallery and historical report labels. |

A new payment/cancellation race test exposed an intermediate defect during development. Owner/order serialization and atomic compare-and-set state transitions fixed it; the final repeated race checks pass. A stock-version conflict also exposed a stale administrator overwrite; version validation and conflict responses now cover it. Earlier failed development attempts are not counted as successful validation. An added anonymous-browser assertion initially expected 403; Spring Security correctly redirects an anonymous visitor to login (302). The assertion was corrected and the full 49-check browser run then passed.

## Automated results

Executed command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/Run-IsolatedMysqlTests.ps1 -Package
```

| Suite | Passed |
|---|---:|
| AdminSecurityTests | 34 |
| CartIsolationTests | 19 |
| OrderSecurityTests | 21 |
| FinalQualityTests | 18 |
| MigrationTests | 3 |
| SecurityConfigTests | 1 |
| DisabledAdminSecurityTests | 1 |
| MiniEcommerceApplicationTests | 1 |
| **Total** | **98** |

The previous 77-test suite was retained and rerun; fixtures/assertions were adapted to decimal types, required product versions and the deliberately retired legacy deletion route. New checks cover precise totals, recovery expiry/revocation, persistent replay, stale/concurrent edits, cancellation, expiry exclusions and payment/cancellation contention. Original stock-contention, CSRF, ownership and rollback scenarios remain exercised.

Local environment: Maven 3.9.15, JDK 21.0.10 compiling release 17, Spring Boot 4.0.6, MySQL 8.4.13. Native Java 17 was unavailable locally. The new Java 17 GitHub Actions workflow has not been run remotely. No coverage percentage or green CI badge is invented.

## Browser and visual results

Executed command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/Run-IsolatedBrowserChecks.ps1 -Screenshots
```

Microsoft Edge **157.0.4341.46**, driven by Playwright Core **1.65.0**: **49 checks passed**. This includes anonymous administrator read/write rejection with a valid CSRF token, legacy financial-route rejection, administrator login/logout and CRUD, public navigation/search/category filtering, independent visitor contexts, foreign cart/order attempts, CSRF rejection, exact checkout, durable replay after a new HTTP session, stale admin edit rejection, invalid payment methods, real pending/confirmed displays, concurrent simulated payment retries, stock deduction once and cancellation restoration. No browser JavaScript exception was observed.

Eleven page types were measured at 390 px and 320 px: home, catalogue, cart, checkout, login, administrator dashboard, add/edit product, add category, payment and confirmation. Measured document widths and navigation bounds passed at both sizes. Current mobile cart/admin captures were visually inspected: stacked cards remain readable and action buttons fit. Desktop and public catalogue/home images were also inspected. This is not an exhaustive accessibility or pixel-regression audit.

Eleven final screenshots are linked in the [gallery](screenshots/README.md); four earlier root-level images were preserved. No credentials, connection settings or personal contact forms appear in the published captures. Only fictitious catalogue data is shown.

## Database changes

Three [versioned migrations](MIGRATIONS.md) cover the original schema, decimal/version/replay constraints and guest-proof expiry. They were applied only to disposable empty databases or explicitly seeded historical fixtures. Fresh migration, fixture upgrade, invalid-data refusal and unmanaged-database refusal passed. No real historical-data compatibility guarantee is claimed; MySQL DDL and independent monetary rounding require a reviewed backup-copy rehearsal before any real migration.

## Files affected

Current working-tree changes include earlier authorized batches as well as this finalization. The complete path inventory is in [CHANGED_FILES.md](CHANGED_FILES.md).

- Business: entities; order/cart/product/category/user services and repositories; `Money`, `Secrets`, `CheckoutDetails`, `GuestCartService`, `GuestOrderService`.
- Security: `config/SecurityConfig.java`, `GuestIdentityFilter`, `ReservationExpiryJob`, existing controllers and CSRF-enabled templates.
- Persistence/build: `pom.xml`, `application.properties`, `db/migration/V1...V3`, Maven Wrapper correction retained.
- UI: templates, `static/css/responsive.css`, themed `error.html` and real screenshots.
- Validation: nine Java test source files, test configuration, isolated MySQL/browser scripts and Java hashing helpers.
- Documentation/repository: README, migration/report/gallery documents, historical report notices, `.gitignore`, GitHub Actions workflow.

## Remaining limits and manual decisions

- Simulated payments only; no real gateway, refunds, delivery integration or customer authentication is implemented.
- Guest recovery cannot work after losing/expiring/revoking its cookie. The original checkout key and matching payload are required for durable replay; deliberately new attempts are separate orders.
- Reservation expiry is optional. Historical abandoned orders are intentionally excluded and need an approved review rather than automatic stock rewriting.
- Historical decimal rounding can require financial reconciliation. Invalid old data blocks migration; it is not automatically repaired.
- Search takes precedence over category when both are provided. Category editing, product image upload and dedicated product detail pages are not implemented.
- Flyway's MySQL verification-version warning, Mockito dynamic-agent warning and JPA open-in-view warning remain documented. Full accessibility testing, independent penetration testing and remote CI execution remain future validation.
- Choose a license if reuse is intended; configure your own local administrator through the README. No real password or hash was generated for publication.

Generated credentials/dependencies/logs/reports remain ignored under `target/`. No generated or ignored file is already tracked. Final diff whitespace checks pass; Git may emit Windows line-ending conversion warnings. No commit, push or remote change was made. Temporary application processes and newly created database containers were stopped by their owning scripts; existing services and databases were not altered.
