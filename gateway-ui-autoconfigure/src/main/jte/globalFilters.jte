@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.inspect.FilterStep
@import java.util.List
@param UiContext ui
@param List<FilterStep> filters
@template.layout(ui = ui, title = "Filtres globaux", content = @`
    <div class="page-head">
        <div>
            <h1>Filtres globaux</h1>
            <p class="muted">Appliqués à toutes les routes, intercalés avec les filtres de chaque route selon leur ordre.</p>
        </div>
    </div>
    <div class="card">
        <table>
            <thead><tr><th class="num">Ordre</th><th>Filtre</th><th>Classe</th></tr></thead>
            <tbody>
            @for(FilterStep filter : filters)
                <tr>
                    <td class="num mono" title="${filter.orderHint()}">${filter.orderText()}</td>
                    <td>${filter.name()}</td>
                    <td class="mono muted">${filter.description()}</td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
`)
