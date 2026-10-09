@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.inspect.RouteView
@import io.github.doriangrelu.gatewayui.internal.inspect.FilterStep
@import io.github.doriangrelu.gatewayui.internal.inspect.Definition
@import java.util.List
@param UiContext ui
@param RouteView route
@param List<FilterStep> pipeline
@template.layout(ui = ui, title = route.id(), content = @`
    <a class="back" href="${ui.url("/routes")}">${ui.message("common.backToRoutes")}</a>
    <div class="page-head">
        <div>
            <h1 class="mono">${route.id()}</h1>
            <p class="muted">
                ${ui.message("route.meta.order", route.order())} ·
                ${ui.message(route.declarative() ? "route.meta.declarative" : "route.meta.java")}
            </p>
        </div>
        <a class="button" href="${ui.url("/tester")}">${ui.message("route.test")}</a>
    </div>

    <section class="flow" aria-label="${ui.message("route.flow")}">
        <div class="flow-node flow-start">
            <span class="flow-label">${ui.message("route.flow.incoming")}</span>
        </div>
        <div class="flow-arrow" aria-hidden="true"></div>

        <div class="flow-node">
            <span class="flow-label">${ui.message("route.flow.predicates")}</span>
            @if(!route.predicateDefinitions().isEmpty())
                <ul class="defs">
                    @for(Definition predicate : route.predicateDefinitions())
                        <li><span class="def-name">${predicate.name()}</span><span class="def-args mono">${predicate.argsText()}</span></li>
                    @endfor
                </ul>
                <p class="hint">${ui.message("route.flow.predicatesHint")}</p>
            @else
                <pre class="raw">${route.predicate()}</pre>
            @endif
        </div>
        <div class="flow-arrow" aria-hidden="true"></div>

        <div class="flow-node">
            <span class="flow-label">${ui.message("route.flow.filters")} <span class="count">${pipeline.size()}</span></span>
            <ol class="chain">
                @for(FilterStep step : pipeline)
                    <li class="${step.global() ? "step step-global" : "step step-route"}">
                        <span class="step-order mono" title="${ui.orderHint(step)}">${ui.orderText(step)}</span>
                        <span class="step-body">
                            <span class="step-name">${step.name()}</span>
                            @if(!step.description().isEmpty())<span class="step-desc mono">${ui.filterDescription(step)}</span>@endif
                        </span>
                        <span class="tag">${ui.message(step.global() ? "filter.global" : "filter.route")}</span>
                    </li>
                @endfor
            </ol>
            <p class="hint">${ui.message("route.flow.filtersHint")}</p>
        </div>
        <div class="flow-arrow" aria-hidden="true"></div>

        <div class="flow-node flow-end">
            <span class="flow-label">${ui.message("route.flow.target")}</span>
            <span class="target mono">${route.uri().toString()}</span>
        </div>
    </section>

    @if(!route.filterDefinitions().isEmpty())
        <section class="card">
            <h2>${ui.message("route.declaredFilters")}</h2>
            <ul class="defs">
                @for(Definition filter : route.filterDefinitions())
                    <li><span class="def-name">${filter.name()}</span><span class="def-args mono">${filter.argsText()}</span></li>
                @endfor
            </ul>
        </section>
    @endif

    @if(!route.metadata().isEmpty())
        <section class="card">
            <h2>${ui.message("route.metadata")}</h2>
            <dl class="kv">
                @for(var entry : route.metadata().entrySet())
                    <dt class="mono">${entry.getKey()}</dt><dd class="mono">${String.valueOf(entry.getValue())}</dd>
                @endfor
            </dl>
        </section>
    @endif
`)
