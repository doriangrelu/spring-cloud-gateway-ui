@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.i18n.Message
@import io.github.doriangrelu.gatewayui.internal.tester.TestRequest
@import io.github.doriangrelu.gatewayui.internal.tester.TestResult
@param UiContext ui
@param TestRequest request
@param TestResult result
@param Message error
@template.layout(ui = ui, title = ui.message("tester.title"), content = @`
    <div class="page-head">
        <div>
            <h1>${ui.message("tester.title")}</h1>
            <p class="muted">${ui.message("tester.intro")}</p>
        </div>
    </div>
    <form class="card tester-form" action="${ui.url("/tester")}" method="get"
          hx-get="${ui.url("/tester")}" hx-target="#tester-result" hx-swap="outerHTML" hx-push-url="true">
        <div class="request-line">
            <select name="method" aria-label="${ui.message("tester.method")}">
                @for(String method : new String[] {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"})
                    <option value="${method}" selected="${method.equals(request.method())}">${method}</option>
                @endfor
            </select>
            <input class="mono" name="host" value="${request.host()}" aria-label="${ui.message("tester.host")}" placeholder="localhost" size="18">
            <input class="mono grow" name="path" value="${"/".equals(request.pathAndQuery()) ? "" : request.pathAndQuery()}"
                   aria-label="${ui.message("tester.path")}" placeholder="/api/orders/42?expand=true" required autofocus>
            <button type="submit">${ui.message("tester.submit")}</button>
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
            </div>
        </details>
    </form>
    @template.testerResult(ui = ui, request = request, result = result, error = error)
`)
