@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import gg.jte.Content
@import java.util.Locale
@import io.github.doriangrelu.gatewayui.internal.web.UiTheme
@param UiContext ui
@param String title
@param Content content
<!doctype html>
<html lang="${ui.lang()}" data-theme="${ui.theme().attribute()}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%-- Pas de feuille de style inline injectée par htmx : la CSP de l'UI l'interdit --%>
    <meta name="htmx-config" content='{"includeIndicatorStyles": false}'>
    <title>${title} · Gateway UI</title>
    <link rel="stylesheet" href="${ui.asset("gateway-ui.css")}">
    <script src="${ui.asset("htmx.min.js")}" defer></script>
</head>
<body>
<header class="topbar">
    <a class="brand" href="${ui.url("/routes")}" title="${ui.message("app.title")}">
        <span class="logo" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18"><path d="M20 3.5C12.5 3.4 5.6 6.9 5.2 14c-.1 1.6.2 3 .8 4.2 1-3.9 3.6-7.1 7.4-9.1-3.1 2.4-5.2 5.6-6 9.5 1.1.6 2.3.9 3.6.9 6.6-.1 9.6-7 9-16Z" fill="currentColor"/><circle cx="4.5" cy="19.5" r="1.6" fill="currentColor"/></svg>
        </span>
        <span class="brand-text">Gateway UI<small>${ui.message("app.subtitle")}</small></span>
    </a>
    <nav>
        <a class="${ui.navClass("routes")}" href="${ui.url("/routes")}">${ui.message("nav.routes")}</a>
        <a class="${ui.navClass("services")}" href="${ui.url("/services")}">${ui.message("nav.services")}</a>
        <a class="${ui.navClass("filters")}" href="${ui.url("/filters")}">${ui.message("nav.filters")}</a>
        <a class="${ui.navClass("tester")}" href="${ui.url("/tester")}">${ui.message("nav.tester")}</a>
    </nav>
    <div class="preferences">
        <nav class="switch" aria-label="${ui.message("theme.label")}">
            @for(UiTheme theme : ui.themes())
                <a href="${ui.themeUrl(theme)}" title="${ui.message("theme." + theme.code())}" aria-label="${ui.message("theme." + theme.code())}"
                   class="${theme == ui.theme() ? "active" : null}" aria-current="${theme == ui.theme() ? "true" : null}"><span class="${"theme-icon theme-icon-" + theme.code()}" aria-hidden="true"></span></a>
            @endfor
        </nav>
        <nav class="switch" aria-label="${ui.message("nav.language")}">
            @for(Locale language : ui.languages())
                <a href="${ui.languageUrl(language)}" hreflang="${language.getLanguage()}" lang="${language.getLanguage()}"
                   class="${language.equals(ui.locale()) ? "active" : null}" aria-current="${language.equals(ui.locale()) ? "true" : null}">${language.getLanguage().toUpperCase()}</a>
            @endfor
        </nav>
    </div>
</header>
<main>
    ${content}
</main>
</body>
</html>
