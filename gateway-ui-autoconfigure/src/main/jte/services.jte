@import dev.gatewayui.web.UiContext
@import dev.gatewayui.inspect.ServiceView
@import dev.gatewayui.inspect.RouteView
@import java.util.List
@param UiContext ui
@param List<ServiceView> services
@template.layout(ui = ui, title = "Services", content = @`
    <div class="page-head">
        <div>
            <h1>Services</h1>
            <p class="muted">Services déclarés dans <code>gateway.ui.services</code> et cibles déduites des URI des routes.</p>
        </div>
    </div>
    @if(services.isEmpty())
        <p class="card empty">Aucun service.</p>
    @endif
    <div class="grid">
        @for(ServiceView service : services)
            <article class="card service">
                <header>
                    <h2>${service.displayName()}</h2>
                    <span class="${service.declared() ? "badge badge-declared" : "badge"}">${service.declared() ? "déclaré" : "déduit"}</span>
                </header>
                @if(service.baseUrl() != null)
                    <p class="mono target">${service.baseUrl()}</p>
                @endif
                @if(service.routes().isEmpty())
                    <p class="empty">Aucune route ne mène à ce service.</p>
                @else
                    <ul class="service-routes">
                        @for(RouteView route : service.routes())
                            <li>
                                <a class="route-link" href="${ui.routeUrl(route.id())}">${route.id()}</a>
                                <span class="mono predicate">${route.predicateSummary()}</span>
                            </li>
                        @endfor
                    </ul>
                @endif
            </article>
        @endfor
    </div>
`)
