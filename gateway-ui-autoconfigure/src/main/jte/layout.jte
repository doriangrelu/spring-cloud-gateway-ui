@import dev.gatewayui.web.UiContext
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
    <a class="brand" href="${ui.url("/routes")}">
        <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path d="M4 6h6l4 6-4 6H4l4-6zM14 6h6l-4 6 4 6h-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>
        <span>Gateway UI</span>
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
