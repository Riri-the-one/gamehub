# Screenshot gallery

Final captures: 2026-11-12, Microsoft Edge, Playwright-driven real application, disposable MySQL fixture. The final report records the browser version and results.

## Desktop

- [Home](desktop/home.png)
- [Catalogue / product cards](desktop/catalogue.png)
- [Administrator dashboard](desktop/admin-dashboard.png)

## Mobile

| View | 390 px | 320 px |
|---|---|---|
| Home | [Image](mobile/home-390.png) | [Image](mobile/home-320.png) |
| Catalogue | [Image](mobile/catalogue-390.png) | [Image](mobile/catalogue-320.png) |
| Guest cart | [Image](mobile/cart-390.png) | [Image](mobile/cart-320.png) |
| Administrator | [Image](mobile/admin-390.png) | [Image](mobile/admin-320.png) |

The eleven files above were captured from the final application. All catalogue data is fictitious. No login credential, token, personal contact form or database connection detail is shown. Public images stop before the author-attribution footer to avoid exposing names in screenshots. Existing root-level screenshots are retained as earlier validation artifacts; the README uses the current desktop/mobile folders.

Reproduce on Windows with Docker, Node.js and Edge:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/Run-IsolatedBrowserChecks.ps1 -Screenshots
```

The script generates temporary credentials locally, records ignored execution results under `target/browser-audit/`, and stops its own application/container. Screenshots are browser captures, not generated illustrations or design mockups.
