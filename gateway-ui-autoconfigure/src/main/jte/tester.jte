@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.i18n.Message
@import io.github.doriangrelu.gatewayui.internal.tester.TestRequest
@import io.github.doriangrelu.gatewayui.internal.tester.TestResult
@import io.github.doriangrelu.gatewayui.internal.realcall.RealCallResult
@param UiContext ui
@param TestRequest request
@param TestResult result
@param Message error
@param boolean realCallEnabled = false
@param boolean realMode = false
@param String csrfHeaders = null
@param RealCallResult callResult = null
@template.layout(ui = ui, title = ui.message("tester.title"), content = @`
    <div class="page-head">
        <div>
            <h1>${ui.message("tester.title")}</h1>
            <p class="muted simulation-only">${ui.message("tester.intro")}</p>
            @if(realCallEnabled)
                <p class="muted real-only">${ui.message("tester.intro.real")}</p>
            @endif
        </div>
    </div>
    <%-- hx-params : ni le mode ni le corps n'entrent dans l'URL de la simulation, qui reste partageable (ADR 0015) --%>
    <form id="tester-form" class="card tester-form" action="${ui.url("/tester")}" method="get"
          hx-get="${ui.url("/tester")}" hx-target="#tester-result" hx-swap="outerHTML" hx-push-url="true"
          hx-params="${realCallEnabled ? "not tester-mode,body" : null}">
        @if(realCallEnabled)
            <%-- Visibilité par CSS (:has) : pas de JavaScript ajouté (ADR 0011) --%>
            <div class="mode-switch" role="radiogroup" aria-label="${ui.message("tester.mode")}">
                <label><input type="radio" name="tester-mode" id="tester-mode-simulation" value="simulation" checked="${!realMode}">
                    ${ui.message("tester.mode.simulation")}</label>
                <label><input type="radio" name="tester-mode" id="tester-mode-real" value="real" checked="${realMode}">
                    ${ui.message("tester.mode.real")}</label>
            </div>
            <p class="alert alert-warning real-only">${ui.message("tester.call.warning")}</p>
        @endif
        <div class="request-line">
            <select name="method" aria-label="${ui.message("tester.method")}">
                @for(String method : new String[] {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"})
                    <option value="${method}" selected="${method.equals(request.method())}">${method}</option>
                @endfor
            </select>
            <input class="mono" name="host" value="${request.host()}" aria-label="${ui.message("tester.host")}" placeholder="localhost" size="18">
            <input class="mono grow" name="path" value="${"/".equals(request.pathAndQuery()) ? "" : request.pathAndQuery()}"
                   aria-label="${ui.message("tester.path")}" placeholder="/api/orders/42?expand=true" required autofocus>
            <button type="submit" class="simulation-only">${ui.message("tester.submit")}</button>
            @if(realCallEnabled)
                <%-- type="button" : la touche Entrée soumet la simulation, jamais un appel réel.
                     hx-params et hx-push-url, hérités du formulaire, sont rétablis : le corps est envoyé, l'URL ne change pas --%>
                <button type="button" class="real-only button-warning" hx-post="${ui.url("/tester/call")}" hx-params="*" hx-push-url="false"
                        hx-target="#tester-call-result" hx-swap="outerHTML" hx-headers="${csrfHeaders}">${ui.message("tester.call.submit")}</button>
            @endif
        </div>
        <details>
            <summary>${ui.message("tester.advanced")}</summary>
            <div class="advanced">
                <label>${ui.message("tester.headers")}
                    <textarea class="mono" name="headers" rows="4" placeholder="Authorization: Bearer xxx&#10;X-Version: 2">${request.headers()}</textarea>
                </label>
                <label>${ui.message("tester.remoteAddress")}
                    <input class="mono" name="remoteAddress" value="${request.remoteAddress()}">
                </label>
                @if(realCallEnabled)
                    <label class="real-only advanced-body">${ui.message("tester.call.body")}
                        <textarea id="tester-call-body" class="mono" name="body" rows="4" placeholder="{&quot;status&quot;: &quot;paid&quot;}">${request.body()}</textarea>
                    </label>
                @endif
            </div>
        </details>
    </form>
    @if(realCallEnabled)
        <div class="real-only">
            @template.testerCallResult(ui = ui, request = request, callResult = callResult,
                    simulatedRouteId = result == null ? null : result.selectedRouteId())
        </div>
    @endif
    <div class="simulation-only">
        @template.testerResult(ui = ui, request = request, result = result, error = error)
    </div>
`)
