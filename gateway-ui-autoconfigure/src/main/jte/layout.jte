@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import gg.jte.Content
@param UiContext ui
@param String title
@param Content content
<!doctype html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} · Gateway UI</title>
    <link rel="stylesheet" href="${ui.asset("gateway-ui.css")}">
    <script src="${ui.asset("htmx.min.js")}" defer></script>
</head>
<body>
<header class="topbar">
    <a class="brand" href="${ui.url("/routes")}" title="Gateway UI pour Spring Cloud Gateway">
        <span class="logo" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18"><path d="M20 3.5C12.5 3.4 5.6 6.9 5.2 14c-.1 1.6.2 3 .8 4.2 1-3.9 3.6-7.1 7.4-9.1-3.1 2.4-5.2 5.6-6 9.5 1.1.6 2.3.9 3.6.9 6.6-.1 9.6-7 9-16Z" fill="currentColor"/><circle cx="4.5" cy="19.5" r="1.6" fill="currentColor"/></svg>
        </span>
        <span class="brand-text">Gateway UI<small>Spring Cloud Gateway</small></span>
    </a>
    <nav>
        <a class="${ui.navClass("routes")}" href="${ui.url("/routes")}">Routes</a>
        <a class="${ui.navClass("services")}" href="${ui.url("/services")}">Services</a>
        <a class="${ui.navClass("filters")}" href="${ui.url("/filters")}">Filtres globaux</a>
        <a class="${ui.navClass("tester")}" href="${ui.url("/tester")}">Testeur</a>
    </nav>
</header>
<main>
    ${content}
</main>
</body>
</html>
