@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.inspect.RouteView
@import java.util.List
@param UiContext ui
@param List<RouteView> routes
@param String query
@template.layout(ui = ui, title = ui.message("routes.title"), content = @`
    <div class="page-head">
        <div>
            <h1>${ui.message("routes.title")}</h1>
            <p class="muted">${ui.message("routes.intro")}</p>
        </div>
        <input class="search" type="search" name="q" value="${query}" placeholder="${ui.message("routes.search")}"
               hx-get="${ui.url("/routes")}" hx-trigger="input changed delay:250ms, search"
               hx-target="#routes-table" hx-swap="outerHTML" hx-push-url="true" autofocus>
    </div>
    @template.routesTable(ui = ui, routes = routes)
`)
