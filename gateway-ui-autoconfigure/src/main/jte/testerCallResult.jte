@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.realcall.RealCallResult
@import io.github.doriangrelu.gatewayui.internal.realcall.RouteComparison
@import io.github.doriangrelu.gatewayui.internal.tester.TestRequest
@param UiContext ui
@param TestRequest request
@param RealCallResult callResult
@param String simulatedRouteId = null
<div id="tester-call-result">
    @if(callResult != null)
        <section class="card call-result">
            <h2>${ui.message("tester.call.result")}</h2>
            @if(callResult.failed())
                <div class="alert alert-error">${ui.message(callResult.error())}</div>
            @else
                <p class="call-status">
                    <span class="${"chip call-status-" + callResult.status() / 100}">${callResult.status()} ${callResult.reason()}</span>
                    <span class="muted">${ui.message("tester.call.duration", String.valueOf(callResult.durationMillis()))}</span>
                </p>
                !{var comparison = new RouteComparison(simulatedRouteId, callResult.routeId());}
                @if(comparison.known())
                    <p class="call-route">
                        @if(comparison.routed())
                            ${ui.message("tester.call.route")} @template.routeRef(ui = ui, id = comparison.actualRouteId(), link = true)
                        @else
                            ${ui.message("tester.call.route.none")}
                        @endif
                        @if(comparison.matches())
                            <span class="chip chip-match">${ui.message("tester.call.route.match")}</span>
                        @elseif(comparison.simulatedRouteId() == null)
                            <span class="chip chip-shadowed">${ui.message("tester.call.route.mismatchNone")}</span>
                        @else
                            <span class="chip chip-shadowed">${ui.message("tester.call.route.mismatch", comparison.simulatedRouteId())}</span>
                        @endif
                    </p>
                    @if(callResult.targetUrl() != null)
                        <p class="call-target mono">${ui.message("tester.call.forwarded")}
                            <span class="method">${request.method()}</span> ${callResult.targetUrl()}</p>
                    @endif
                @endif
                @if(callResult.unauthorized())
                    <p class="alert alert-warning">${ui.message("tester.call.hint.unauthorized")}</p>
                @endif
                @if(!callResult.headers().isEmpty())
                    <h3>${ui.message("tester.call.headers")}</h3>
                    <dl class="kv">
                        @for(var header : callResult.headers())
                            <dt class="mono">${header.getKey()}</dt><dd class="mono">${header.getValue()}</dd>
                        @endfor
                    </dl>
                @endif
                <h3>${ui.message("tester.call.responseBody")}</h3>
                @if(callResult.body().binary())
                    <p class="muted">${ui.message("tester.call.body.binary", String.valueOf(callResult.body().receivedBytes()))}</p>
                @elseif(callResult.body().receivedBytes() == 0)
                    <p class="muted">${ui.message("tester.call.body.empty")}</p>
                @else
                    <pre class="mono call-body">${callResult.body().text()}</pre>
                @endif
                @if(callResult.body().truncated())
                    <p class="muted">${ui.message("tester.call.body.truncated")}</p>
                @endif
            @endif
            @if(!"127.0.0.1".equals(request.remoteAddress()))
                <p class="notes">${ui.message("tester.call.remoteAddress", request.remoteAddress())}</p>
            @endif
        </section>
    @endif
</div>
