@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.inspect.ServiceView
@import io.github.doriangrelu.gatewayui.internal.inspect.RouteView
@import java.util.List
@param UiContext ui
@param List<ServiceView> services
@template.layout(ui = ui, title = ui.message("services.title"), content = @`
    <div class="page-head">
        <div>
            <h1>${ui.message("services.title")}</h1>
            <p class="muted">${ui.message("services.intro")}</p>
        </div>
    </div>
    @if(services.isEmpty())
        <p class="card empty">${ui.message("services.empty")}</p>
    @endif
    <div class="grid">
        @for(ServiceView service : services)
            <article class="card service">
                <header>
                    <h2>${service.displayName()}</h2>
                    <span class="${service.declared() ? "badge badge-declared" : "badge"}">${ui.message(service.declared() ? "services.declared" : "services.discovered")}</span>
                </header>
                @if(service.baseUrl() != null)
                    <p class="mono target">${service.baseUrl()}</p>
                @endif
                @if(service.routes().isEmpty())
                    <p class="empty">${ui.message("services.noRoute")}</p>
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
