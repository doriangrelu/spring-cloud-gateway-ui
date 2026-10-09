@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@import io.github.doriangrelu.gatewayui.internal.web.CsrfToken
@import java.util.Optional
@param UiContext ui
@param Optional<CsrfToken> csrf
@template.layout(ui = ui, title = ui.message("editor.title"), content = @`
    <%-- Données de la page pour les scripts : URL de l'API, jeton CSRF de Spring Security s'il est actif --%>
    <div id="editor" class="editor" data-api="${ui.url("/api/editor")}" data-catalog="${ui.url("/catalog")}"
         data-csrf-header="${csrf.map(CsrfToken::headerName).orElse(null)}" data-csrf-token="${csrf.map(CsrfToken::token).orElse(null)}">
        <div class="page-head">
            <div>
                <h1>${ui.message("editor.title")}</h1>
                <p class="muted">${ui.message("editor.intro")}</p>
            </div>
            <button type="button" id="reset-workspace" class="secondary small" hidden>${ui.message("editor.reset")}</button>
        </div>
        <p class="notice" id="restored" hidden>${ui.message("editor.restored")}</p>
        <p class="alert alert-error" id="load-error" hidden>${ui.message("editor.loadError")}</p>
        <noscript><p class="alert alert-error">${ui.message("editor.noscript")}</p></noscript>

        <div class="editor-layout">
            <aside class="card editor-sidebar" id="sidebar" aria-label="${ui.message("editor.sidebar")}"></aside>
            <section class="editor-center">
                <div class="card">
                    <div class="context" id="context"></div>
                    <div id="canvas" class="editor-canvas" aria-label="${ui.message("editor.canvas")}"></div>
                </div>
            </section>
            <aside class="card editor-panel" id="panel" aria-label="${ui.message("editor.panel")}"></aside>
        </div>

        <%-- Même formulaire et même résultat que la page « Testeur », appliqués à la route éditée (#39) --%>
        <section class="card editor-tester" id="live-test" aria-labelledby="editor-test-title" hidden>
            <h2 id="editor-test-title">${ui.message("editor.test")}</h2>
            <form class="tester-form" id="editor-test-form">
                <div class="request-line">
                    <select name="method" aria-label="${ui.message("tester.method")}">
                        @for(String method : new String[] {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"})
                            <option value="${method}">${method}</option>
                        @endfor
                    </select>
                    <input class="mono" name="host" aria-label="${ui.message("tester.host")}" placeholder="localhost" size="18">
                    <input class="mono grow" name="path" aria-label="${ui.message("tester.path")}" placeholder="/api/orders/42?expand=true">
                </div>
                <label class="check"><input type="checkbox" name="context"> ${ui.message("editor.test.context")}</label>
                <details>
                    <summary>${ui.message("tester.advanced")}</summary>
                    <div class="advanced">
                        <label>${ui.message("tester.headers")}
                            <textarea class="mono" name="headers" rows="4" placeholder="Authorization: Bearer xxx&#10;X-Version: 2"></textarea>
                        </label>
                        <label>${ui.message("tester.remoteAddress")}
                            <input class="mono" name="remoteAddress">
                        </label>
                    </div>
                </details>
            </form>
            <ul class="lint" id="advice" aria-live="polite"></ul>
            <div id="editor-test-result" aria-live="polite"></div>
        </section>

        <section class="card">
            <div class="yaml-head">
                <h2>${ui.message("editor.export")}</h2>
                <nav class="switch" aria-label="${ui.message("editor.export.mode")}">
                    <a href="#" data-export="current">${ui.message("editor.export.current")}</a>
                    <a href="#" data-export="full">${ui.message("editor.export.full")}</a>
                    <a href="#" data-export="changes">${ui.message("editor.export.changes")}</a>
                </nav>
                <button type="button" id="copy-yaml">${ui.message("editor.export.copy")}</button>
            </div>
            <pre class="yaml mono" id="yaml" tabindex="0"></pre>
        </section>

        <dialog id="palette" class="palette" aria-labelledby="palette-title">
            <h2 id="palette-title"></h2>
            <input id="palette-input" type="search" placeholder="${ui.message("editor.palette.search")}" autocomplete="off"
                   role="combobox" aria-expanded="true" aria-controls="palette-list" aria-autocomplete="list">
            <ul id="palette-list" role="listbox" aria-labelledby="palette-title"></ul>
            <p class="hint">${ui.message("editor.palette.keys")}</p>
        </dialog>

        <%-- Textes des scripts, traduits : aucun texte affiché n'est écrit en dur dans le JavaScript (ADR 0009) --%>
        <div id="editor-messages" hidden>
            @for(String key : ui.messageKeys("editor.js."))
                <span data-key="${key.substring("editor.js.".length())}">${ui.message(key)}</span>
            @endfor
        </div>
    </div>
    <script type="module" src="${ui.asset("editor/main.js")}"></script>
`)
