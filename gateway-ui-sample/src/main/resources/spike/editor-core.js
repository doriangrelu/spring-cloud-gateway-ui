/*
 * SPIKE #28 : orchestration de l'éditeur (panneau des routes, contexte, canevas, aide, test en direct, export).
 */
const Editor = (() => {
    const STATUS = {
        unchanged: ['inchangée', ''], modified: ['modifiée', 'chip-shadowed'], new: ['nouvelle', 'chip-match'],
        removed: ['retirée', 'chip-error'], java: ['Java, lecture seule', '']
    };
    let selection = { kind: 'target', index: 0 };
    let exportMode = 'current';
    let testPath = null;

    const el = id => document.getElementById(id);
    const make = (tag, className, text) => {
        const element = document.createElement(tag);
        if (className) {
            element.className = className;
        }
        if (text != null) {
            element.textContent = text;
        }
        return element;
    };
    const button = (label, onClick, className) => {
        const b = make('button', className || 'secondary', label);
        b.type = 'button';
        b.addEventListener('click', onClick);
        return b;
    };
    const entry = () => Workspace.selected();
    const route = () => (entry() && !entry().java && !entry().removed ? entry().current : null);
    const items = kind => (kind === 'predicate' ? route().predicates : route().filters);

    // Panneau des routes : celles de la Gateway, puis les nouvelles
    function renderSidebar() {
        const sidebar = el('sidebar');
        sidebar.replaceChildren();
        const groups = [['Routes de la Gateway', e => e.original || e.java], ['Nouvelles routes', e => !e.original && !e.java]];
        groups.forEach(([title, filter]) => {
            sidebar.append(make('h3', 'side-title', title));
            const list = make('ul', 'side-list');
            Workspace.entries().filter(filter).forEach(e => {
                const status = Workspace.status(e);
                const item = make('li', e === entry() ? 'active' : '');
                const link = button('', () => { Workspace.select(e.key); selection = { kind: 'target', index: 0 }; testPath = null; render(); }, 'side-item');
                link.append(make('span', 'mono', e.java ? e.id : e.current.id), make('span', 'chip ' + STATUS[status][1], STATUS[status][0]));
                item.append(link);
                list.append(item);
            });
            sidebar.append(list);
        });
        sidebar.append(button('+ Nouvelle route', () => { Workspace.create(); selection = { kind: 'target', index: 0 }; testPath = null; render(); }, 'side-new'));
    }

    function renderContext() {
        const e = entry();
        const context = el('context');
        context.replaceChildren();
        if (!e) {
            return;
        }
        const status = Workspace.status(e);
        const label = { new: 'Nouvelle route', unchanged: 'Route existante', modified: 'Route existante, modifiée',
            removed: 'Route retirée de la configuration', java: 'Route déclarée en Java' }[status];
        const title = make('div', 'context-title');
        title.append(make('span', 'context-label', label), make('h2', 'mono', e.java ? e.id : e.current.id));
        const actions = make('div', 'context-actions');
        if (route()) {
            actions.append(button('+ Prédicat', () => Palette.open('predicate', name => add('predicate', name))),
                button('+ Filtre', () => Palette.open('filter', name => add('filter', name)), 'primary'),
                button('Dupliquer', () => { Workspace.duplicate(e.key); selection = { kind: 'target', index: 0 }; render(); }));
        }
        if (status === 'modified' || status === 'removed') {
            actions.append(button(status === 'removed' ? 'Rétablir' : '↺ Annuler les modifications', () => { Workspace.reset(e.key); render(); }));
        }
        if (!e.java && status !== 'removed') {
            actions.append(button(e.original ? 'Retirer de la configuration' : 'Supprimer', () => { Workspace.remove(e.key); selection = { kind: 'target', index: 0 }; render(); }, 'danger'));
        }
        context.append(title, actions);
    }

    function renderCanvas() {
        const e = entry();
        if (route()) {
            CanvasRenderer.render(route(), selection);
            return;
        }
        const message = !e ? 'Aucune route.' : e.java
            ? 'Cette route est déclarée en Java (DSL) : sans définition déclarative, elle ne peut être ni éditée ni exportée en YAML.'
            : 'Route retirée : elle n\'apparaîtra pas dans l\'export de la configuration complète. « Rétablir » pour la récupérer.';
        el('canvas').replaceChildren(make('p', 'empty', message));
    }

    function help(kind, name) {
        const doc = Catalog.describe(kind, name);
        const box = make('div', 'help');
        const head = make('div', 'help-head');
        head.append(make('span', 'tag', doc.category), make('span', 'help-kind', kind === 'predicate' ? 'Prédicat' : 'Filtre'));
        box.append(head, make('p', 'help-summary', doc.summary));
        if (doc.details) {
            box.append(make('p', 'help-details', doc.details));
        }
        if (doc.example) {
            box.append(make('pre', 'help-example mono', doc.example));
        }
        const link = make('a', 'help-link', 'Documentation de Spring Cloud Gateway ↗');
        link.href = doc.doc;
        link.target = '_blank';
        link.rel = 'noopener';
        box.append(link);
        return { box, args: doc.args || {} };
    }

    function input(label, value, onInput, hint) {
        const wrapper = make('label', 'panel-field', label);
        const field = make('input', 'mono');
        field.value = value == null ? '' : value;
        field.addEventListener('input', () => onInput(field.value));
        wrapper.append(field);
        if (hint) {
            wrapper.title = hint;
            wrapper.append(make('small', '', hint));
        }
        return wrapper;
    }

    function renderPanel() {
        const panel = el('panel');
        panel.replaceChildren();
        if (!route()) {
            panel.append(make('p', 'hint', 'Sélectionnez une route éditable.'));
            return;
        }
        if (selection.kind === 'target') {
            panel.append(make('h2', '', 'Route'),
                make('p', 'hint', 'Identifiant, service appelé et ordre d\'évaluation (le plus petit est évalué en premier).'),
                input('Identifiant', route().id, v => update('target', 0, 'id', v), 'Unique dans la configuration.'),
                input('URI cible', route().uri, v => update('target', 0, 'uri', v), 'Seuls le schéma, l\'hôte et le port sont utilisés : le chemin vient de la requête.'),
                input('Ordre', route().order, v => update('target', 0, 'order', v), '0 par défaut. Une valeur plus élevée est évaluée plus tard.'));
            return;
        }
        const { kind, index } = selection;
        const item = items(kind)[index];
        const { box, args } = help(kind, item.name);
        panel.append(make('h2', '', item.name), box);
        Model.fields(kind, item).forEach(name => panel.append(input(name, item.values[name], v => update(kind, index, name, v), args[name])));
        const actions = make('div', 'panel-actions');
        actions.append(button('← Avant', () => move(kind, index, index - 1)), button('Après →', () => move(kind, index, index + 1)),
            button('Supprimer', () => remove(kind, index), 'danger'));
        panel.append(actions);
    }

    function defaultPath(r) {
        const path = r.predicates.find(p => p.name === 'Path');
        const pattern = path ? (path.values.patterns || '').split(',')[0].trim() : '/';
        return pattern.replace(/\/\*\*$/, '/42').replace(/\{\*?(\w+)(:[^}]*)?\}/g, '42').replace(/\*/g, 'x') || '/';
    }

    function renderTest() {
        const box = el('live-test');
        box.replaceChildren();
        if (!route()) {
            return;
        }
        if (testPath === null) {
            testPath = defaultPath(route());
        }
        const line = make('div', 'test-line');
        const field = make('input', 'mono grow');
        field.value = testPath;
        field.setAttribute('aria-label', 'Chemin à tester');
        field.addEventListener('input', () => { testPath = field.value; renderResult(); });
        line.append(make('span', 'test-label', 'Tester'), make('span', 'method', 'GET'), field);
        box.append(line, make('div', 'test-result'), make('ul', 'lint'));
        renderResult();
    }

    function renderResult() {
        const result = document.querySelector('#live-test .test-result');
        const lintList = document.querySelector('#live-test .lint');
        if (!result || !route()) {
            return;
        }
        const outcome = Simulator.test(route(), testPath || '/', 'GET');
        result.replaceChildren();
        if (!outcome.matched) {
            result.append(make('p', 'test-ko', '✘ Cette route ne prend pas ce chemin (' + (outcome.reason || '') + ')'));
        } else {
            result.append(make('p', 'test-ok', '✔ ' + outcome.target));
            outcome.steps.forEach(step => result.append(make('p', 'test-step mono', step.simulated
                ? `${step.name} : ${step.before} → ${step.after}` : `${step.name} : effet non simulé${step.error ? ' (' + step.error + ')' : ''}`)));
            const vars = Object.entries(outcome.vars || {});
            if (vars.length) {
                result.append(make('p', 'test-step mono', 'Variables : ' + vars.map(([k, v]) => k + '=' + v).join(', ')));
            }
        }
        const others = Workspace.entries().filter(e => e !== entry() && e.current && !e.removed).map(e => e.current);
        lintList.replaceChildren(...Simulator.lint(route(), others).map(a => make('li', 'lint-' + a.level, a.text)));
    }

    function renderExport() {
        document.querySelectorAll('[data-export]').forEach(b => b.classList.toggle('active', b.dataset.export === exportMode));
        el('yaml').textContent = Workspace.yaml(exportMode);
    }

    function render() {
        renderSidebar();
        renderContext();
        renderCanvas();
        renderPanel();
        renderTest();
        renderExport();
    }

    // Après une saisie : tout sauf le formulaire, pour ne pas perdre le focus
    function refresh() {
        Workspace.save();
        renderSidebar();
        renderContext();
        CanvasRenderer.refreshLabels(route());
        renderResult();
        renderExport();
    }

    function select(kind, index) {
        selection = { kind, index };
        render();
    }

    function add(kind, name) {
        const values = {};
        Model.factory(kind, name).fields.forEach(field => { values[field] = ''; });
        items(kind).push({ name, values });
        Workspace.save();
        select(kind, items(kind).length - 1);
    }

    function remove(kind, index) {
        items(kind).splice(index, 1);
        Workspace.save();
        select('target', 0);
    }

    function move(kind, from, to) {
        const list = items(kind);
        if (to < 0 || to >= list.length || from === to) {
            return;
        }
        const [item] = list.splice(from, 1);
        list.splice(to, 0, item);
        Workspace.save();
        select(kind, to);
    }

    function update(kind, index, field, value) {
        if (kind === 'target') {
            route()[field] = value;
        } else {
            items(kind)[index].values[field] = value;
        }
        refresh();
    }

    return {
        render, select, move,
        setExportMode(mode) { exportMode = mode; renderExport(); }
    };
})();
