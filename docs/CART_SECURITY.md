> Historical batch report. Results, setup and limitations below describe an earlier stage and may be superseded. See the [current final report](FINAL_REPORT.md), [README](../README.md) and [migration policy](MIGRATIONS.md).

# Guest cart isolation — batch 2

> Subsequent step 3A implements owner-checked orders, atomic checkout and guarded simulated payments without migration. Current results are 77 passing MySQL tests and 22 full-purchase Edge checks; see [order security](ORDER_SECURITY.md). Older checkout/payment findings below describe the earlier baseline.

Implemented and validated on `fix/admin-security`, 2026-11-12. No entity mapping, existing database, checkout/order/payment business method, or administrator credential configuration was changed in this batch.

## Identity and ownership

Each HTTP session receives a newly persisted guest `Utilisateurs` record with role `Client`, without registration, password or email input. Its ID is stored only in the server session as `gamehub.guestUserId.v2`. Historical clients are never selected as a default. The cart is created when needed, rather than selected from another visitor's data.

`GuestCartService` is the HTTP-facing boundary. It obtains the owner from the session, then invokes owner-scoped cart operations. Submitted `panierId` and `utilisateurId` parameters are not used to select a cart. Product IDs refer to the public catalogue and must exist; line IDs must belong to the resolved owner's cart. Missing and foreign lines both return 404 without deleting anything.

The older `currentUser` session attribute is refreshed from the authoritative ID for compatibility with existing checkout rendering. It cannot grant ownership: an old session containing only the formerly shared user receives a new guest identity. Existing shared carts and historical orders are neither transferred nor deleted.

Two tabs sharing one browser session share one cart. Independent sessions have independent carts, even when adding the same product. Expiration, cookie loss or administrator logout creates a fresh guest on the next visit; anonymous cart recovery across sessions is not implemented. Guest records are retained, with no automatic purge.

## Writes, validation and concurrency

- `/panier` is a GET view. `/panier/ajouter`, `/panier/supprimer/{id}`, and `/panier/vider` require POST.
- Spring Security CSRF remains enabled globally. Removal and clear controls are now Thymeleaf POST forms using the existing visual classes. GET removal/clear return 405 and do not mutate data.
- Quantities must be integers from 1 through 99, including the accumulated quantity for one product. Empty, malformed, negative, zero, excessively large and overflowing input is rejected with 400. Omitting quantity still defaults to 1.
- Accumulated quantity must not exceed the product's currently observed stock; missing stock is rejected. This is an add-time validation, not a stock reservation or a checkout concurrency fix.
- Guest writes run in service transactions. They first acquire `PESSIMISTIC_WRITE` on the owner row, which exists even before a cart does. All guest add/remove/clear and cart-creation operations follow this lock order. This serializes first-cart creation and line merges without adding a column or index.
- First identity creation is synchronized using Spring's HTTP session mutex, registered with `HttpSessionMutexListener`. Identity initialization completes before the transactional cart operation starts.
- Privileged legacy administration methods remain separate and role-protected; they are not guest cart endpoints. The lock protocol does not claim to cover the unchanged checkout or arbitrary administrative mutations.

## Session security

Servlet sessions use cookie-only tracking, `HttpOnly`, and `SameSite=Lax`. Edge verified distinct session cookies for the two visitors. Session fixation protection remains managed by Spring Security: actual administrator login changed the session ID while retaining the guest cart. Logout invalidated the session, and its former cart was not adopted by a subsequent guest.

Local validation used HTTP on loopback, so the cookie was not forced to `Secure`. HTTPS deployments should enable `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`. The implementation uses in-memory Servlet sessions; distributed deployment would need an explicitly configured shared session store or session affinity and separate validation.

## MySQL schema

No migration is required or supplied. Inspection was limited to the newly created disposable MySQL 8.4.13 database:

- `utilisateurs`, `paniers` and `ligne_paniers` use InnoDB.
- The existing `@OneToOne` mapping creates a unique index on `paniers.utilisateur_id` in a fresh schema.
- `ligne_paniers` has its primary key and foreign-key indexes; no new `(panier, produit)` uniqueness constraint was added. Guest writers avoid duplicate lines through the owner-lock protocol.

No production/historical schema was inspected or altered. Adding further constraints or deduplicating historical rows would require separate approval. For the browser validation, the application ran with `ddl-auto=validate`; schema generation during tests concerned only new disposable databases.

## Reproducing the verified MySQL tests

With Docker Desktop running and a JDK configured, execute from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/Run-IsolatedMysqlTests.ps1
```

The script creates a uniquely named `mysql:8.4` container, a fresh `gamehub_cart_tests` database, random temporary credentials, memory-backed database storage, and a dynamic loopback-only port. It runs the Maven test suite and stops only its own verified container in `finally`. It restores process environment variables and does not remove files or containers. Stopping a memory-backed test container discards its temporary database; it must not be used to retain data. A stopped container and the downloaded image remain in Docker.

`DatabaseTestSupport` overrides each Spring test context with the temporary MySQL connection when `GAMEHUB_TEST_MYSQL_URL` is present. It accepts only a loopback JDBC URL for the explicitly named test database. Without that opt-in, the earlier H2 defaults remain available; no H2 execution is claimed for this batch. Credentials must never be committed. Maven and Docker need network access on their first downloads.

## Automated results

**56 tests passed: 0 failures, 0 errors, 0 skipped.** The original 37 tests were rerun with MySQL, together with 19 new cart cases. Compilation used Maven 3.9.15, JDK 21.0.10 and Java `release 17`. Existing Lombok builder-default warnings remain.

New checks cover separate sessions; same-session continuity; identical products in independent carts; injected owner/cart IDs on reads and writes; foreign/nonexistent line rejection; owner removal/clear; CSRF; GET rejection; malformed, empty and out-of-range quantities; stock and accumulated limits; missing products; the default quantity; old-session rejection; expiration; and checkout rendering without submission.

Concurrency cases start eight workers together: first identity/cart creation, adding one product, competing additions at the 99-item limit, and mixed clear/add operations. Results verify one identity/cart, no duplicate lines or lost increments, and exactly one accepted increment from 98 to 99. These are targeted tests, not a load/performance benchmark.

An initial run exposed an unboxing error for an empty quantity; it was corrected before the successful runs. A Spring Boot 4 web-server API compilation mismatch was also corrected. Neither failure is counted as a passing run.

## Actual Edge validation

Microsoft Edge 157.0.4341.46 ran against the temporary MySQL with two independent browser contexts. **18 browser checks passed**, plus a separate actual click on the owner-removal button:

- Administrator login/product creation still worked with environment-configured test credentials.
- The two visitor cookies were distinct and had `HttpOnly` / `SameSite=Lax`.
- A's product was absent from B's cart; B could not delete A's line even with a valid B token.
- Quantities remained independent, and clearing B left A unchanged.
- Missing/invalid CSRF and invalid quantities were rejected; GET mutations returned 405.
- Eight simultaneous HTTP additions merged into one line with the expected quantity.
- The checkout link and rendering remained usable, without submitting a command or payment.
- Administrator login rotated the session ID without losing the cart. Logout produced a new guest cart, and the other visitor remained unable to access administration.
- No JavaScript exceptions were recorded. A desktop cart diagnostic capture was visually inspected; it remains under ignored `target/cart-audit/` and is not a new public screenshot.

## Changed files and remaining scope

- `config/GuestSessionConfig.java`, `services/GuestCartService.java`: new session setup and guest boundary.
- `services/impl/UtilisateurServiceImpl.java`, `services/UtilisateurService.java`: guest identity creation/resolution and retirement of the global-client method.
- `services/PaniersService.java`, `services/impl/PaniersServiceImpl.java`: owner-scoped transactional operations, locking and quantity validation.
- `repositories/UtilisateursRepositorie.java`, `PaniersRepositorie.java`, `LignePaniersRepositorie.java`: locking and scoped lookups.
- `services/LignePaniersService.java`, `services/impl/LignePaniersServiceImpl.java`: retirement of the old unrestricted guest-add path; administrator CRUD remains.
- `controllers/PaniersController.java`, `templates/panier.html`, `config/SecurityConfig.java`: session-derived ownership and CSRF-protected POST operations.
- `CartIsolationTests.java`, `DatabaseTestSupport.java`, and the three existing Spring context test classes: new coverage and temporary MySQL support, preserving the original assertions.
- `tools/Run-IsolatedMysqlTests.ps1`, README and technical documentation: reproducible tests and verified behavior.

No new cart regression was detected in the checked flows. Historical order ownership, transactional checkout, payment access/idempotence, retention of abandoned guests, and the previously reported mobile-layout issues remain separate work. The cart still uses existing `Double` totals and existing entity mappings. No migration, cleanup of existing data, commit or push was performed. Validation JVMs and newly created MySQL test containers were stopped afterward; no pre-existing container was stopped or removed.
