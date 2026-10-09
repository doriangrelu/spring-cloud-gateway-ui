@import io.github.doriangrelu.gatewayui.web.UiContext
@import io.github.doriangrelu.gatewayui.inspect.RouteView
@import io.github.doriangrelu.gatewayui.inspect.FilterStep
@import io.github.doriangrelu.gatewayui.inspect.Definition
@import java.util.List
@param UiContext ui
@param RouteView route
@param List<FilterStep> pipeline
@template.layout(ui = ui, title = route.id(), content = @`
    <a class="back" href="${ui.url("/routes")}">← Toutes les routes</a>
    <div class="page-head">
        <div>
            <h1 class="mono">${route.id()}</h1>
            <p class="muted">
                Ordre ${route.order()} ·
                @if(route.declarative()) Déclarée en configuration @else Déclarée en Java (DSL) @endif
            </p>
        </div>
        <a class="button" href="${ui.url("/tester")}">Tester une requête</a>
    </div>

    <section class="flow" aria-label="Parcours d'une requête">
        <div class="flow-node flow-start">
            <span class="flow-label">Requête entrante</span>
        </div>
        <div class="flow-arrow" aria-hidden="true"></div>

        <div class="flow-node">
            <span class="flow-label">Prédicats</span>
            @if(!route.predicateDefinitions().isEmpty())
                <ul class="defs">
                    @for(Definition predicate : route.predicateDefinitions())
                        <li><span class="def-name">${predicate.name()}</span><span class="def-args mono">${predicate.argsText()}</span></li>
                    @endfor
                </ul>
                <p class="hint">Tous doivent correspondre.</p>
            @else
                <pre class="raw">${route.predicate()}</pre>
            @endif
        </div>
        <div class="flow-arrow" aria-hidden="true"></div>

        <div class="flow-node">
            <span class="flow-label">Chaîne de filtres <span class="count">${pipeline.size()}</span></span>
            <ol class="chain">
                @for(FilterStep step : pipeline)
                    <li class="${step.global() ? "step step-global" : "step step-route"}">
                        <span class="step-order mono" title="${step.orderHint()}">${step.orderText()}</span>
                        <span class="step-body">
                            <span class="step-name">${step.name()}</span>
                            @if(!step.description().isEmpty())<span class="step-desc mono">${step.description()}</span>@endif
                        </span>
                        <span class="tag">${step.global() ? "global" : "route"}</span>
                    </li>
                @endfor
            </ol>
            <p class="hint">Exécutés dans cet ordre vers le service, puis en ordre inverse sur la réponse.</p>
        </div>
        <div class="flow-arrow" aria-hidden="true"></div>

        <div class="flow-node flow-end">
            <span class="flow-label">Cible</span>
            <span class="target mono">${route.uri().toString()}</span>
        </div>
    </section>

    @if(!route.filterDefinitions().isEmpty())
        <section class="card">
            <h2>Filtres déclarés sur la route</h2>
            <ul class="defs">
                @for(Definition filter : route.filterDefinitions())
                    <li><span class="def-name">${filter.name()}</span><span class="def-args mono">${filter.argsText()}</span></li>
                @endfor
            </ul>
        </section>
    @endif

    @if(!route.metadata().isEmpty())
        <section class="card">
            <h2>Métadonnées</h2>
            <dl class="kv">
                @for(var entry : route.metadata().entrySet())
                    <dt class="mono">${entry.getKey()}</dt><dd class="mono">${String.valueOf(entry.getValue())}</dd>
                @endfor
            </dl>
        </section>
    @endif
`)
