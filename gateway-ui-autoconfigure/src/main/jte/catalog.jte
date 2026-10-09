@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.web.CatalogView
@import java.util.List
@import java.util.Map
@param UiContext ui
@param List<CatalogView.Group> groups
@template.layout(ui = ui, title = ui.message("catalog.title"), content = @`
    <div class="page-head">
        <div>
            <h1>${ui.message("catalog.title")}</h1>
            <p class="muted">${ui.message("catalog.intro")}</p>
        </div>
    </div>
    <nav class="card catalog-index" aria-label="${ui.message("catalog.index")}">
        @for(CatalogView.Group group : groups)
            <div>
                <h3 class="side-title">${ui.message("catalog.kind." + group.kind())} · ${group.category()}</h3>
                <p>
                    @for(CatalogView entry : group.entries())
                        <a class="${entry.documented() ? "mono" : "mono muted"}" href="#${entry.anchor()}">${entry.name()}</a>
                    @endfor
                </p>
            </div>
        @endfor
    </nav>
    @for(CatalogView.Group group : groups)
        <section>
            <h2>${ui.message("catalog.kind." + group.kind())} · ${group.category()}</h2>
            <div class="grid catalog-grid">
                @for(CatalogView entry : group.entries())
                    <article class="card catalog-entry" id="${entry.anchor()}">
                        <header>
                            <h3 class="mono">${entry.name()}</h3>
                            @if(entry.custom())
                                <span class="chip chip-shadowed">${ui.message("catalog.badge.custom")}</span>
                            @elseif(!entry.documented())
                                <span class="chip">${ui.message("catalog.badge.undocumented")}</span>
                            @endif
                        </header>
                        <p class="catalog-summary">${entry.summary()}</p>
                        @if(!entry.details().isEmpty())
                            <p class="muted">${entry.details()}</p>
                        @endif
                        @if(!entry.args().isEmpty())
                            <dl class="kv">
                                @for(Map.Entry<String, String> arg : entry.args().entrySet())
                                    <dt class="mono">${arg.getKey()}</dt>
                                    <dd>${arg.getValue().isEmpty() ? "—" : arg.getValue()}</dd>
                                @endfor
                            </dl>
                        @endif
                        @if(!entry.example().isEmpty())
                            <pre class="mono catalog-example">${entry.example()}</pre>
                        @endif
                        @if(entry.documentation() != null)
                            <a href="${entry.documentation()}" target="_blank" rel="noopener noreferrer">${ui.message("catalog.documentation")}</a>
                        @endif
                    </article>
                @endfor
            </div>
        </section>
    @endfor
`)
