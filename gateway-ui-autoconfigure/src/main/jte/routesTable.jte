@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.inspect.RouteView
@import java.util.List
@param UiContext ui
@param List<RouteView> routes
<div id="routes-table" class="card">
    @if(routes.isEmpty())
        <p class="empty">${ui.message("routes.empty")}</p>
    @else
        <table>
            <thead>
            <tr>
                <th class="num">${ui.message("routes.col.order")}</th>
                <th>${ui.message("routes.col.route")}</th>
                <th>${ui.message("routes.col.predicates")}</th>
                <th>${ui.message("routes.col.target")}</th>
                <th class="num">${ui.message("routes.col.filters")}</th>
            </tr>
            </thead>
            <tbody>
            @for(RouteView route : routes)
                <tr>
                    <td class="num mono">${route.order()}</td>
                    <td>
                        <a class="route-link" href="${ui.routeUrl(route.id())}">${route.id()}</a>
                        @if(!route.declarative())<span class="badge badge-java" title="${ui.message("routes.java.title")}">${ui.message("routes.java")}</span>@endif
                    </td>
                    <td class="mono predicate">${route.predicateSummary()}</td>
                    <td class="mono">${route.uri().toString()}</td>
                    <td class="num">${route.filters().size()}</td>
                </tr>
            @endfor
            </tbody>
        </table>
    @endif
</div>
