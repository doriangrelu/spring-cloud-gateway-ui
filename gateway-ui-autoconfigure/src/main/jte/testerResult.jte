@import io.github.doriangrelu.gatewayui.web.UiContext
@import io.github.doriangrelu.gatewayui.tester.TestRequest
@import io.github.doriangrelu.gatewayui.tester.TestResult
@import io.github.doriangrelu.gatewayui.tester.TestResult.Evaluation
@import io.github.doriangrelu.gatewayui.tester.TestResult.Outcome
@import io.github.doriangrelu.gatewayui.tester.TestResult.Step
@import io.github.doriangrelu.gatewayui.tester.TestResult.StepStatus
@param UiContext ui
@param TestRequest request
@param TestResult result
@param String error
<div id="tester-result">
    @if(error != null)
        <div class="alert alert-error">${error}</div>
    @elseif(result != null)
        @if(result.selectedRouteId() == null)
            <div class="verdict verdict-none">
                <span class="verdict-title">Aucune route ne correspond</span>
                <span class="muted">La Gateway répondrait 404 à <span class="mono">${request.method()} ${request.pathAndQuery()}</span>.</span>
            </div>
        @else
            <div class="verdict verdict-match">
                <span class="verdict-title">Route <a class="route-link" href="${ui.routeUrl(result.selectedRouteId())}">${result.selectedRouteId()}</a></span>
                <span class="verdict-call mono">
                    <span class="method">${request.method()}</span> ${result.simulation().targetUrl()}
                </span>
            </div>

            <div class="grid grid-2">
                <section class="card">
                    <h2>Transformations</h2>
                    @if(result.simulation().steps().isEmpty())
                        <p class="empty">Aucun filtre sur cette route : le chemin est transmis tel quel.</p>
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
                                        <div class="sim-path mono muted">${step.pathAfter()} (inchangé)</div>
                                    @endif
                                    @if(step.note() != null)<div class="sim-note">${step.note()}</div>@endif
                                </li>
                            @endfor
                        </ol>
                    @endif
                    @if(!result.simulation().uriVariables().isEmpty())
                        <h3>Variables extraites</h3>
                        <dl class="kv">
                            @for(var variable : result.simulation().uriVariables().entrySet())
                                <dt class="mono">${variable.getKey()}</dt><dd class="mono">${variable.getValue()}</dd>
                            @endfor
                        </dl>
                    @endif
                </section>

                <section class="card">
                    <h2>Requête transmise</h2>
                    <p class="mono target">${request.method()} ${result.simulation().targetUrl()}</p>
                    <dl class="kv">
                        @for(var header : result.simulation().headers())
                            <dt class="mono">${header.getKey()}</dt><dd class="mono">${header.getValue()}</dd>
                        @endfor
                    </dl>
                    <ul class="notes">
                        @for(String note : result.simulation().notes())
                            <li>${note}</li>
                        @endfor
                    </ul>
                </section>
            </div>
        @endif

        <section class="card">
            <h2>Évaluation des routes</h2>
            <table>
                <thead><tr><th class="num">Ordre</th><th>Route</th><th>Résultat</th><th>Prédicat</th></tr></thead>
                <tbody>
                @for(Evaluation evaluation : result.evaluations())
                    <tr class="${evaluation.selected() ? "row-selected" : ""}">
                        <td class="num mono">${evaluation.order()}</td>
                        <td><a class="route-link" href="${ui.routeUrl(evaluation.routeId())}">${evaluation.routeId()}</a></td>
                        <td>
                            @if(evaluation.selected())
                                <span class="chip chip-match">retenue</span>
                            @elseif(evaluation.shadowed())
                                <span class="chip chip-shadowed" title="Correspond, mais une route évaluée avant a été retenue">masquée</span>
                            @elseif(evaluation.outcome() == Outcome.ERROR)
                                <span class="chip chip-error" title="${evaluation.error()}">erreur</span>
                            @else
                                <span class="chip">ne correspond pas</span>
                            @endif
                        </td>
                        <td class="mono predicate">${evaluation.predicate()}</td>
                    </tr>
                @endfor
                </tbody>
            </table>
        </section>
    @else
        <p class="placeholder muted">Saisis un chemin pour voir quelle route le prend en charge.</p>
    @endif
</div>
