@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.tester.TestRequest
@import io.github.doriangrelu.gatewayui.internal.tester.TestResult
@param UiContext ui
@param TestRequest request
@param TestResult result
@param String error
@template.layout(ui = ui, title = "Testeur de routes", content = @`
    <div class="page-head">
        <div>
            <h1>Testeur de routes</h1>
            <p class="muted">Quelle route reçoit cette requête, et que transmet la Gateway au service ? Aucune requête n'est réellement envoyée.</p>
        </div>
    </div>
    <form class="card tester-form" action="${ui.url("/tester")}" method="get"
          hx-get="${ui.url("/tester")}" hx-target="#tester-result" hx-swap="outerHTML" hx-push-url="true">
        <div class="request-line">
            <select name="method" aria-label="Méthode HTTP">
                @for(String method : new String[] {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"})
                    <option value="${method}" selected="${method.equals(request.method())}">${method}</option>
                @endfor
            </select>
            <input class="mono" name="host" value="${request.host()}" aria-label="Hôte" placeholder="localhost" size="18">
            <input class="mono grow" name="path" value="${"/".equals(request.pathAndQuery()) ? "" : request.pathAndQuery()}"
                   aria-label="Chemin" placeholder="/api/orders/42?expand=true" required autofocus>
            <button type="submit">Tester</button>
        </div>
        <details>
            <summary>En-têtes et IP cliente</summary>
            <div class="advanced">
                <label>En-têtes, un par ligne
                    <textarea class="mono" name="headers" rows="4" placeholder="Authorization: Bearer xxx&#10;X-Version: 2">${request.headers()}</textarea>
                </label>
                <label>IP cliente (prédicat RemoteAddr)
                    <input class="mono" name="remoteAddress" value="${request.remoteAddress()}">
                </label>
            </div>
        </details>
    </form>
    @template.testerResult(ui = ui, request = request, result = result, error = error)
`)
