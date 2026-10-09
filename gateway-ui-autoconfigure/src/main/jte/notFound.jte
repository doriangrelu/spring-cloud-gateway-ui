@import dev.gatewayui.web.UiContext
@param UiContext ui
@param String message
@template.layout(ui = ui, title = "Introuvable", content = @`
    <a class="back" href="${ui.url("/routes")}">← Toutes les routes</a>
    <div class="card empty">${message}</div>
`)
