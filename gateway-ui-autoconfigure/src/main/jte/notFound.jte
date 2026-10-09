@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.i18n.Message
@param UiContext ui
@param Message message
@template.layout(ui = ui, title = ui.message("common.notFound"), content = @`
    <a class="back" href="${ui.url("/routes")}">${ui.message("common.backToRoutes")}</a>
    <div class="card empty">${ui.message(message)}</div>
`)
