@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.inspect.FilterStep
@import java.util.List
@param UiContext ui
@param List<FilterStep> filters
@template.layout(ui = ui, title = ui.message("filters.title"), content = @`
    <div class="page-head">
        <div>
            <h1>${ui.message("filters.title")}</h1>
            <p class="muted">${ui.message("filters.intro")}</p>
        </div>
    </div>
    <div class="card">
        <table>
            <thead>
            <tr>
                <th class="num">${ui.message("filters.col.order")}</th>
                <th>${ui.message("filters.col.filter")}</th>
                <th>${ui.message("filters.col.class")}</th>
            </tr>
            </thead>
            <tbody>
            @for(FilterStep filter : filters)
                <tr>
                    <td class="num mono" title="${ui.orderHint(filter)}">${ui.orderText(filter)}</td>
                    <td>${filter.name()}</td>
                    <td class="mono muted">${ui.filterDescription(filter)}</td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
`)
