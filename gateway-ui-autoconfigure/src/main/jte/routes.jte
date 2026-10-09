@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.inspect.RouteView
@import java.util.List
@param UiContext ui
@param List<RouteView> routes
@param String query
@template.layout(ui = ui, title = "Routes", content = @`
    <div class="page-head">
        <div>
            <h1>Routes</h1>
            <p class="muted">Dans l'ordre d'évaluation : la première route dont les prédicats correspondent reçoit la requête.</p>
        </div>
        <input class="search" type="search" name="q" value="${query}" placeholder="Filtrer par id, prédicat ou URI…"
               hx-get="${ui.url("/routes")}" hx-trigger="input changed delay:250ms, search"
               hx-target="#routes-table" hx-swap="outerHTML" hx-push-url="true" autofocus>
    </div>
    @template.routesTable(ui = ui, routes = routes)
`)
