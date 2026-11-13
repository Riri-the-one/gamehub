> Historical batch report. Results, setup and limitations below describe an earlier stage and may be superseded. See the [current final report](FINAL_REPORT.md), [README](../README.md) and [migration policy](MIGRATIONS.md).

# Functional and visual validation

> Subsequent step 3A implements owner-checked orders, atomic checkout and guarded simulated payments without migration. Current results are 77 passing MySQL tests and 22 full-purchase Edge checks; see [order security](ORDER_SECURITY.md). Older checkout/payment findings below describe the earlier baseline.

> This report records the validation after batch 1. The subsequent guest-cart changes, 56 passing MySQL tests and two-session Edge checks are documented in [cart security](CART_SECURITY.md); its shared-cart findings below describe the earlier baseline.

Date: 2026-11-12. Branch: `fix/admin-security`. This validation exercised the actual application in Microsoft Edge 157.0.4341.46 using Playwright Core and a real, isolated MySQL 8.4.13 server. No application code was changed during this validation.

## Isolation

Docker Desktop was initially stopped. After starting it, a new uniquely named MySQL container was created using `mysql:8.4`, with `/var/lib/mysql` on a temporary memory filesystem and a dynamically assigned port bound only to `127.0.0.1`. No existing database, container volume, or database account was used. A new database and restricted application user were initialized inside that container.

GameHub ran on `127.0.0.1:18087`. Datasource settings and a randomly generated administrator password/BCrypt hash were passed through process environment variables. No credentials were added to tracked configuration, screenshots, or this report. Temporary credentials and automation dependencies remain only under ignored `target/functional-audit/`; they must not be published. The screenshots contain only synthetic product/category data created through the application forms.

## Results

The browser script completed **20 successful checks**:

| Area | Actual result |
| --- | --- |
| Startup | Spring Boot connected to MySQL 8.4.13, initialized its schema in the new database, and started normally. |
| Public pages | Home, catalogue, and login returned HTTP 200. Navigation from home to catalogue worked; public navigation targets returned HTTP 200. |
| Product rendering | Three demonstration products appeared in the public catalogue with descriptions, prices, stock indicators and existing fallback icons. |
| Search/filter | Name search returned one expected product; category filtering returned two expected products. |
| Administrator login | Successful browser login using `GAMEHUB_ADMIN_EMAIL` and `GAMEHUB_ADMIN_PASSWORD_HASH` supplied only to the application process. |
| Anonymous protection | `/admin` redirected to login. An administrative POST without CSRF returned 403; a POST with a valid anonymous CSRF token still redirected to login without creating data. |
| Authenticated CSRF | Administrative POSTs with missing or invalid tokens returned 403. Valid form submissions succeeded. |
| Products | Created a disposable product, edited its name/price, verified the updated dashboard row, and deleted it through the confirmation/form flow. GET deletion returned 405. |
| Categories | Created and deleted a disposable category through the forms. Two other demonstration categories were created for screenshots. |
| Logout | POST without CSRF returned 403. Dashboard logout with a valid token succeeded; subsequent `/admin` access redirected to login. |
| Browser errors | No page JavaScript exceptions were recorded during the administrator workflow. This is not a complete network-resource audit. |

The prior **37 passing automated tests** were not rerun in this documentation/screenshot-only pass. Browser checks complement those H2 tests with actual HTTP/form interactions and MySQL persistence; they do not constitute a complete automated MySQL integration suite.

## Visual inspection and screenshots

Four real desktop screenshots were captured with a 1440 × 1000 browser viewport and inspected:

- [Home](screenshots/home.png): navigation, hero, and demonstration product cards.
- [Catalogue](screenshots/catalogue.png): search/category controls and three product cards.
- [Product card](screenshots/product-card.png): an actual rendered catalogue card, not a separate product-details page.
- [Administrator dashboard](screenshots/admin-dashboard.png): product/category tables and action buttons, captured after authentication without showing the login form or credentials.

Public-page captures stop before the footer to omit existing author names. No text, product graphics, or UI state was fabricated or retouched. Products deliberately use the application's existing fallback icons because the repository contains no product photos. No session cookies, visible CSRF values, account email, passwords, hashes, or personal customer records appear in the published images.

Desktop pages rendered with the existing dark/neon design; the administrator POST buttons and logout control remained usable. This was a visual inspection, not a pixel comparison against a historical screenshot.

## Remaining issues and scope limits

1. **Mobile layout overflow:** at viewport width 390 px, the document width measured 667 px on home and 620 px on catalogue. The header/navigation and home hero are clipped or overflow. Diagnostic mobile screenshots remain only under ignored `target/functional-audit/`. Public templates and shared CSS have no diff against the original checkout; this is a pre-existing responsive-layout issue, not a first-batch regression. No CSS fix was made.
2. **No dedicated product-details route/view:** products are presented as cards on home/catalogue. The product screenshot accurately represents that implemented interface.
3. **No working category-edit interface:** the dashboard offers creation/deletion only. The legacy edit route references an absent template. Category modification could not be validated as a supported browser flow; implementing it would require a separate functional change.
4. **Images:** products have no bundled photographs or upload endpoint; fallback icons are visible in these captures. No photo-upload capability is claimed.
5. **Existing business risks:** shared guest/customer carts, ownership checks, transactional checkout, and repeat-payment handling remain outside this batch. No order/payment was submitted and no cart business fix was made.
6. Existing Lombok warnings and missing legacy CRUD views remain. This pass detected no regression directly attributable to the first security batch, so no application correction was necessary.

The application JVM and the newly created MySQL container were stopped after validation. The container was not removed, and no existing container or database was stopped or deleted. Docker Desktop remains running. No commit or push was performed.
