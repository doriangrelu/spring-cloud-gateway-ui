@import io.github.doriangrelu.gatewayui.web.UiContext
@import io.github.doriangrelu.gatewayui.inspect.RouteView
@import java.util.List
@param UiContext ui
@param List<RouteView> routes
<div id="routes-table" class="card">
    @if(routes.isEmpty())
        <p class="empty">Aucune route.</p>
    @else
        <table>
            <thead>
            <tr><th class="num">Ordre</th><th>Route</th><th>Prédicats</th><th>Cible</th><th class="num">Filtres</th></tr>
            </thead>
            <tbody>
            @for(RouteView route : routes)
                <tr>
                    <td class="num mono">${route.order()}</td>
                    <td>
                        <a class="route-link" href="${ui.routeUrl(route.id())}">${route.id()}</a>
                        @if(!route.declarative())<span class="badge badge-java" title="Route déclarée en Java (DSL)">Java</span>@endif
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
