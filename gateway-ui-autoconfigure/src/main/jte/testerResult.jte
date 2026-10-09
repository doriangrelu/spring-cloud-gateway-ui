@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.i18n.Message
@import io.github.doriangrelu.gatewayui.internal.tester.TestRequest
@import io.github.doriangrelu.gatewayui.internal.tester.TestResult
@import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Evaluation
@import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Outcome
@import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Step
@import io.github.doriangrelu.gatewayui.internal.tester.TestResult.StepStatus
@param UiContext ui
@param TestRequest request
@param TestResult result
@param Message error
<div id="tester-result">
    @if(error != null)
        <div class="alert alert-error">${ui.message(error)}</div>
    @elseif(result != null)
        @if(result.selectedRouteId() == null)
            <div class="verdict verdict-none">
                <span class="verdict-title">${ui.message("result.none")}</span>
                <span class="muted">${ui.message("result.none.detail")} <span class="mono">${request.method()} ${request.pathAndQuery()}</span>.</span>
            </div>
        @else
            <div class="verdict verdict-match">
                <span class="verdict-title">${ui.message("result.route")} <a class="route-link" href="${ui.routeUrl(result.selectedRouteId())}">${result.selectedRouteId()}</a></span>
                <span class="verdict-call mono">
                    <span class="method">${request.method()}</span> ${result.simulation().targetUrl()}
                </span>
            </div>

            <div class="grid grid-2">
                <section class="card">
                    <h2>${ui.message("result.transformations")}</h2>
                    @if(result.simulation().steps().isEmpty())
                        <p class="empty">${ui.message("result.noFilter")}</p>
                    @else
                        <ol class="sim-steps">
                            @for(Step step : result.simulation().steps())
                                <li class="${"sim-step sim-" + step.status().name().toLowerCase()}">
                                    <div class="sim-head">
                                        <span class="step-name">${step.filter()}</span>
                                        @if(!step.args().isEmpty())<span class="step-desc mono">${step.args()}</span>@endif
                                    </div>
                                    @if(step.changedPath())
                                        <div class="sim-path mono"><s>${step.pathBefore()}</s> → <strong>${step.pathAfter()}</strong></div>
                                    @elseif(step.status() == StepStatus.APPLIED && step.pathBefore() != null && step.note() == null)
                                        <div class="sim-path mono muted">${step.pathAfter()} ${ui.message("result.unchanged")}</div>
                                    @endif
                                    @if(step.note() != null)<div class="sim-note">${ui.message(step.note())}</div>@endif
                                </li>
                            @endfor
                        </ol>
                    @endif
                    @if(!result.simulation().uriVariables().isEmpty())
                        <h3>${ui.message("result.variables")}</h3>
                        <dl class="kv">
                            @for(var variable : result.simulation().uriVariables().entrySet())
                                <dt class="mono">${variable.getKey()}</dt><dd class="mono">${variable.getValue()}</dd>
                            @endfor
                        </dl>
                    @endif
                </section>

                <section class="card">
                    <h2>${ui.message("result.forwarded")}</h2>
                    <p class="mono target">${request.method()} ${result.simulation().targetUrl()}</p>
                    <dl class="kv">
                        @for(var header : result.simulation().headers())
                            <dt class="mono">${header.getKey()}</dt><dd class="mono">${header.getValue()}</dd>
                        @endfor
                    </dl>
                    <ul class="notes">
                        @for(Message note : result.simulation().notes())
                            <li>${ui.message(note)}</li>
                        @endfor
                    </ul>
                </section>
            </div>
        @endif

        <section class="card">
            <h2>${ui.message("result.evaluation")}</h2>
            <table>
                <thead>
                <tr>
                    <th class="num">${ui.message("result.col.order")}</th>
                    <th>${ui.message("result.col.route")}</th>
                    <th>${ui.message("result.col.outcome")}</th>
                    <th>${ui.message("result.col.predicate")}</th>
                </tr>
                </thead>
                <tbody>
                @for(Evaluation evaluation : result.evaluations())
                    <tr class="${evaluation.selected() ? "row-selected" : ""}">
                        <td class="num mono">${evaluation.order()}</td>
                        <td><a class="route-link" href="${ui.routeUrl(evaluation.routeId())}">${evaluation.routeId()}</a></td>
                        <td>
                            @if(evaluation.selected())
                                <span class="chip chip-match">${ui.message("result.selected")}</span>
                            @elseif(evaluation.shadowed())
                                <span class="chip chip-shadowed" title="${ui.message("result.shadowed.title")}">${ui.message("result.shadowed")}</span>
                            @elseif(evaluation.outcome() == Outcome.ERROR)
                                <span class="chip chip-error" title="${evaluation.error()}">${ui.message("result.error")}</span>
                            @else
                                <span class="chip">${ui.message("result.noMatch")}</span>
                            @endif
                        </td>
                        <td class="mono predicate">${evaluation.predicate()}</td>
                    </tr>
                @endfor
                </tbody>
            </table>
        </section>
    @else
        <p class="placeholder muted">${ui.message("tester.placeholder")}</p>
    @endif
</div>
