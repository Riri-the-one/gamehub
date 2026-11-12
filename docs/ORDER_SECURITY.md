> Historical batch report. Results, setup and limitations below describe an earlier stage and may be superseded. See the [current final report](FINAL_REPORT.md), [README](../README.md) and [migration policy](MIGRATIONS.md).

# Orders, checkout and simulated payments — step 3A

Branch: `fix/admin-security`. Validation date: 2026-11-12. This step changes application logic only: no entity/column mapping, monetary Java type, existing database, or migration was modified. Payment remains a pedagogical simulation with no gateway integration or real debit.

## Ownership and public routes

`GuestOrderService` resolves the owner exclusively from the guest identity established by batch 2. The browser supplies only contact details, a submission token, an order ID where applicable, and a payment-method enum value. Owner, price, total and status parameters cannot override server values.

`GET /paiement/{id}`, `GET /confirmation/{id}` and `POST /commander/payer` use owner-scoped service lookups. A missing or foreign order returns 404, including when the visitor possesses a valid CSRF token. Historical orders belonging to the earlier shared client are never adopted by a new guest, email address, or forged legacy `currentUser` attribute.

GET routes display data; checkout creation and payment require POST and the existing global CSRF protection. A paid/non-pending order's payment page redirects to its current confirmation state. Confirmation displays the stored order status and only claims simulated payment success when an approved payment and a compatible order status both exist. Existing CSS and page layout are retained; text now describes the true state rather than always promising successful payment/shipping.

## Atomic creation and stock

`CommandesServiceImpl.passerCommande()` is transactional. Contact updates, order insertion, price/quantity snapshots, stock deductions, order lines and removal of cart lines commit together or roll back together.

The operation acquires the same owner-row write lock as guest cart writers, then locks products in ascending ID order. This serializes checkout against cart add/remove/clear and coordinates competing guest purchases of the same stock. Products are refreshed under lock, including when the request's persistence context had loaded them earlier. Stock deduction also uses an atomic conditional SQL update (`quantity >= requested`); a zero-row update causes the entire order transaction to roll back.

Before writes, the service rejects empty carts, invalid or excessive quantities (including aggregate quantities above 99 for one product), missing products/stock, insufficient stock, null/negative/non-finite prices, and non-finite totals. Contact fields must be nonblank and no longer than 255 characters; the email receives a basic server-side format check. Zero-priced products remain permitted. Prices come from the database at submission time, not the browser. The original `Double` types and calculations remain; this step introduces no decimal conversion or new rounding guarantee.

Stock continues to be deducted **when creating the order, before payment**. Paying does not deduct it a second time. An abandoned pending order therefore keeps its stock deduction indefinitely. Cancellation, expiry and automatic stock restoration are not implemented in this step, and no historical stock was repaired.

## Best-effort duplicate submission protection

The checkout form contains a server-generated random token held in the HTTP session. `GuestOrderService` synchronizes submissions using the session mutex. After the transaction commits, the session remembers that token's resulting order ID. Repeating the same completed attempt returns the same owned order, including if another product has subsequently been added to the cart; the new cart is not consumed by that replay.

Failed transactions leave the attempt retryable. Visiting checkout after a completed attempt creates a new token; an older token is then rejected with 409. Missing or foreign-session tokens are rejected. The database owner lock also prevents two concurrent raw service calls from consuming the same cart twice.

This is **not durable idempotence**. A crash between database commit and session update, lost/expired session, or replacement of the remembered attempt cannot be recovered using a persisted key. The implementation does not persist a cart snapshot or guarantee that the price shown earlier still matches the price at submission; submission uses validated current prices. Durable keys and stronger stale-checkout detection belong to step 3B.

## Simulated payment transaction

The payment service locks an owned order, validates the method before writes, and validates the stored amount/state. It atomically inserts an approved simulated payment and changes `EN_ATTENTE` to `CONFIRMEE`.

An already approved payment with the same method/amount and a confirmed or subsequently progressed order is a no-op. Concurrent repeats are serialized on the order row. A different method, cancelled/incompatible status, or inconsistent existing payment returns 409. An invalid method returns 400 without a state change; foreign/missing orders return 404. A persistence error rolls back the confirmation. No real card data is processed or stored.

## Verification

The reproducible command remains:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/Run-IsolatedMysqlTests.ps1
```

It creates its own memory-backed MySQL container on a loopback-only dynamic port and stops only that container afterward. All 56 earlier tests and 21 new order tests were executed against MySQL 8.4.13: **77 passed, 0 failures, 0 errors, 0 skipped**. No H2 test execution is claimed for this step. Compilation used Maven 3.9.15, JDK 21.0.10 and Java `release 17`; the existing Lombok warnings remain.

The new tests cover:

- Ownership on consultation/payment, nonexistent orders and formerly shared guest identity.
- Atomic successful creation and historical unit-price snapshots.
- A forced persistence failure after order/line insertion and stock deduction, immediately before cart clearing: order, lines, stock and contact changes roll back, and the original cart/token remain usable.
- A later unavailable product, invalid prices/quantities and contact data, and browser-injected monetary/owner/status parameters.
- Competing orders for the last unit: exactly one success, stock zero, no partial losing order.
- Repeated and concurrent submissions, including direct service calls and checkout versus cart clearing.
- Concurrent/repeated payments, method changes, invalid methods, cancellation, payment-write failure and CSRF.
- Confirmation rendering for pending and approved states.

The early concurrency test exposed a defect in the first stock-decrement implementation; the conditional SQL deduction corrected it before the passing runs. Test-harness compilation/unboxing and exception-translation assertions were also corrected. No failed run is counted as a success.

Microsoft Edge 157.0.4341.46 completed **22 browser checks** against a separate new MySQL database. The actual UI flow created a synthetic product, added it to A's cart, submitted fictitious delivery details, paid with the CASH simulation, and displayed the confirmed order. A separate visitor B received 404 for consultation/payment and could not see the address. Missing/invalid CSRF, invalid payment method, checkout replay, four repeated payment requests, method-change rejection, unchanged stock after repeats, cart emptying and administrator boundaries were checked. No JavaScript exceptions were recorded. A local diagnostic screenshot was visually inspected and remains under ignored `target/order-audit/`; no new public capture containing delivery information was added.

An initial Edge assertion inspected CSS-uppercase rendered text with a mixed-case comparison. It was corrected to inspect semantic HTML text; the successful full rerun is the reported result.

## Files changed for step 3A

- `controllers/CommandesController.java`: session-owned public actions, checkout token and actual confirmation state; privileged legacy CRUD remains separate.
- `services/CheckoutDetails.java`, `GuestOrderService.java`: contact-only input and HTTP session boundary/replay protection.
- `services/CommandesService.java`, `services/impl/CommandesServiceImpl.java`: owner-scoped reads, atomic checkout/payment, validation and locking.
- `repositories/CommandesRepositorie.java`, `PaiementsRepositorie.java`, `ProduitsRepositorie.java`: scoped queries, row locks and conditional stock deduction.
- `templates/checkout.html`, `templates/confirmation.html`: hidden token and truthful status text without CSS redesign.
- `OrderSecurityTests.java`, README and technical reports: verified behavior, safety limits and remaining work.

## Step 3B — separate authorization required

1. Migrate prices/totals to `BigDecimal`/MySQL `DECIMAL` and define rounding, precision, bounds and conversion of historical values. `Double` precision limitations remain today.
2. Add a durable submission key and unique constraint, with compatibility for historical orders. No such column or key was added in 3A.
3. Audit payment uniqueness and historical inconsistent records before proposing constraints or repairs. No existing record was deduplicated or reassigned.
4. Address stale administrative product edits and privileged legacy CRUD that can bypass lifecycle invariants. Checkout/product row locks and conditional decrements protect the new guest purchase path; they do not make an intentionally submitted absolute stock replacement safe against stale administrator forms. Versioning or changes to those administrative workflows need separate review/approval.
5. Decide any release of stock for abandoned/cancelled orders. Automatic expiry, recovery after guest-session loss and stock restoration would change behavior and are not silently introduced.

Application and new browser/test database containers were stopped after validation. No existing data was modified, no entity migration was made, no file/container was deleted, and no commit or push was performed.
