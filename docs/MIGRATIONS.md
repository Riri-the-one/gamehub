# Migration and historical-data safety

This is the current migration policy. No existing database was accessed or migrated during finalization.

## Versioned schema

| Migration | Change |
|---|---|
| V1 | Reconstructs the original eight-table schema, including its legacy DOUBLE monetary columns and enum ordinals. Used as the starting point for fresh installations and disposable historical fixtures. |
| V2 | Converts product, order, order-line and payment amounts to DECIMAL(19,2); adds product versioning, nullable checkout-key/fingerprint and guest-proof hash; enforces unique checkout key, guest hash and payment per order. |
| V3 | Adds nullable guest-token expiry. No recovery proof is created for historical customers. |

Hibernate uses `ddl-auto=validate`. Flyway has automatic baseline disabled and clean disabled. Startup against an unmanaged nonempty database is deliberately refused. Existing enum ordinals are preserved; no historical owner is reassigned.

## Executed validation

On independently created MySQL 8.4.13 containers only:

- New schema: all three migrations followed by Hibernate validation and the complete application suite.
- Historical fixture: V1 schema containing fictitious product/order/line/payment records, upgraded to latest. Records survive; 0.105 rounds to 0.11; new ownership/replay metadata remains null.
- Incompatible fixture: a negative historical price blocks V2 before monetary conversion. Its value remains unchanged.
- Unmanaged fixture: Flyway refuses adoption and leaves its pre-existing probe table untouched.

V2 also explicitly rejects duplicate payments per order and negative/out-of-range amounts. That duplicate-payment branch is an SQL preflight rule, not a separately executed fixture assertion. Invalid legacy stock, contacts and orphaned references require additional review; the migration is not a data-cleaning tool.

## Before any real historical migration

No command in the verification scripts performs these operations on an existing database. A future migration requires a separate, reviewed procedure:

1. Back up the original database and prove that restoration works into a **new isolated copy**.
2. Compare that copy with V1: table names, types, constraints, enum ordinals, character set and existing customizations. Do not assume any database matches the fixture.
3. Review null/negative/out-of-range amounts, duplicate payments, invalid quantities, owners and orphaned records. Resolve inconsistencies only through an approved business/data decision.
4. Reconcile historical order totals, line subtotals and payment amounts. Converting each DOUBLE independently rounds to two decimals; it does not reconstruct historical invoices or silently recompute them.
5. Only on the isolated copy, explicitly establish the verified V1 baseline through a reviewed Flyway operation, then test V2/V3 and the application. Never enable blanket automatic baselining as a shortcut.
6. Obtain approval for a production/real-data maintenance window, restore plan and reconciliation before touching the original.

MySQL DDL is not fully transactional. A failure may leave partial DDL or a preflight procedure behind; do not blindly repair migration history or rerun against real data. Inspect the disposable copy and recreate that copy if necessary. The verified path is a clean migration, not a guaranteed in-place rollback.

## Historical access and reservations

Historical rows receive null checkout keys, fingerprints, guest hashes and expiry. Their old shared customer is never adopted by a new visitor. They cannot be recovered by knowing an email or numeric ID. The reservation expiry worker excludes historical orders without a checkout key. Paid historical amounts are not recalculated.

New pending orders reserve stock at checkout. Explicit owned cancellation restores stock once. The optional expiry worker defaults to off; when enabled it uses a 30-minute threshold and a bounded 100-order sweep. This is a prototype reservation policy, not a background payment/refund system.

## Compatibility caveat

Flyway 11.14.1 emits a warning that MySQL 8.4 is newer than its verified MySQL version. The migrations and application tests were nevertheless actually executed successfully on MySQL 8.4.13. A future dependency upgrade should repeat these fixtures.
