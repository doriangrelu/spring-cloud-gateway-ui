/*
 * SPIKE #28 (branche jetable) : modèle de route, formulaires générés et YAML, communs aux deux moteurs de rendu.
 */
const Editor = (() => {
    const state = { factories: { predicates: [], filters: [] }, route: null, selected: { kind: 'target', index: 0 }, renderer: null };

    const factory = (kind, name) => (kind === 'predicate' ? state.factories.predicates : state.factories.filters)
        .find(f => f.name === name) || { name, fields: [], shortcutType: 'DEFAULT' };

    // Les arguments déclarés en forme raccourcie arrivent indexés (_genkey_0...) : on les rattache aux champs de la fabrique
    function fromDefinition(kind, definition) {
        const f = factory(kind, definition.name);
        const raw = Object.entries(definition.args || {});
        const generated = raw.every(([key]) => key.startsWith('_genkey_'));
        const values = {};
        if (!generated) {
            raw.forEach(([key, value]) => { values[key] = value; });
        } else if (f.shortcutType.startsWith('GATHER_LIST')) {
            let items = raw.map(([, value]) => value);
            if (f.shortcutType === 'GATHER_LIST_TAIL_FLAG' && /^(true|false)$/.test(items[items.length - 1] || '')) {
                values[f.fields[1]] = items.pop();
            }
            values[f.fields[0]] = items.join(', ');
        } else {
            raw.forEach(([, value], index) => { values[f.fields[index] || ('arg' + index)] = value; });
        }
        return { name: definition.name, values };
    }

    function load(definition) {
        state.route = {
            id: definition.id, uri: definition.uri, order: definition.order || 0,
            predicates: definition.predicates.map(p => fromDefinition('predicate', p)),
            filters: definition.filters.map(f => fromDefinition('filter', f))
        };
        select('target', 0);
    }

    function blank() {
        load({ id: 'nouvelle-route', uri: 'http://service.namespace.svc.cluster.local:8080', order: 0,
            predicates: [{ name: 'Path', args: { _genkey_0: '/api/**' } }], filters: [] });
    }

    const items = kind => (kind === 'predicate' ? state.route.predicates : state.route.filters);

    function select(kind, index) {
        state.selected = { kind, index };
        render();
    }

    function add(kind, name) {
        const f = factory(kind, name);
        const values = {};
        f.fields.forEach(field => { values[field] = ''; });
        items(kind).push({ name, values });
        select(kind, items(kind).length - 1);
    }

    function remove(kind, index) {
        items(kind).splice(index, 1);
        select('target', 0);
    }

    function move(kind, from, to) {
        const list = items(kind);
        if (to < 0 || to >= list.length || from === to) {
            return;
        }
        const [item] = list.splice(from, 1);
        list.splice(to, 0, item);
        select(kind, to);
    }

    function update(kind, index, field, value) {
        if (kind === 'target') {
            state.route[field] = value;
        } else {
            items(kind)[index].values[field] = value;
        }
        renderYaml();
        state.renderer.refreshLabels(state.route);
    }

    // Forme raccourcie : Nom=valeur1, valeur2 ; les listes (GATHER_LIST) sont déjà séparées par des virgules
    function shortcut(kind, item) {
        const f = factory(kind, item.name);
        const fields = f.fields.length ? f.fields : Object.keys(item.values);
        const values = fields.map(field => (item.values[field] || '').trim());
        while (values.length && values[values.length - 1] === '') {
            values.pop();
        }
        return values.length ? item.name + '=' + values.join(', ') : item.name;
    }

    // Spring résout ${...} dans le YAML : un $ { littéral (RewritePath) doit s'écrire $\{
    function yamlValue(text) {
        const escaped = text.replace(/\$\{/g, '$\\{');
        return /(: | #|^[*&!|>'"%@`{}\[\]])/.test(escaped) ? "'" + escaped.replace(/'/g, "''") + "'" : escaped;
    }

    function yaml(route) {
        const lines = ['spring:', '  cloud:', '    gateway:', '      server:', '        webflux:', '          routes:',
            '            - id: ' + yamlValue(route.id), '              uri: ' + yamlValue(route.uri)];
        if (String(route.order) !== '0' && String(route.order) !== '') {
            lines.push('              order: ' + route.order);
        }
        [['predicates', 'predicate'], ['filters', 'filter']].forEach(([key, kind]) => {
            if (route[key].length) {
                lines.push('              ' + key + ':');
                route[key].forEach(item => lines.push('                - ' + yamlValue(shortcut(kind, item))));
            }
        });
        return lines.join('\n');
    }

    function renderYaml() {
        document.getElementById('yaml').textContent = yaml(state.route);
    }

    function field(label, value, onInput, hint) {
        const wrapper = document.createElement('label');
        wrapper.className = 'panel-field';
        wrapper.append(label);
        const input = document.createElement('input');
        input.className = 'mono';
        input.value = value == null ? '' : value;
        input.addEventListener('input', () => onInput(input.value));
        wrapper.append(input);
        if (hint) {
            const small = document.createElement('small');
            small.textContent = hint;
            wrapper.append(small);
        }
        return wrapper;
    }

    function button(label, onClick, className) {
        const b = document.createElement('button');
        b.type = 'button';
        b.textContent = label;
        b.className = className || 'secondary';
        b.addEventListener('click', onClick);
        return b;
    }

    function renderPanel() {
        const panel = document.getElementById('panel');
        panel.replaceChildren();
        const { kind, index } = state.selected;
        const title = document.createElement('h2');
        if (kind === 'target') {
            title.textContent = 'Route';
            panel.append(title,
                field('Identifiant', state.route.id, v => update('target', 0, 'id', v)),
                field('URI cible', state.route.uri, v => update('target', 0, 'uri', v)),
                field('Ordre', state.route.order, v => update('target', 0, 'order', v)));
            return;
        }
        const item = items(kind)[index];
        const f = factory(kind, item.name);
        title.textContent = item.name;
        const subtitle = document.createElement('p');
        subtitle.className = 'hint';
        subtitle.textContent = (kind === 'predicate' ? 'Prédicat' : 'Filtre') + ' n° ' + (index + 1)
            + ' · formulaire généré depuis la fabrique de la Gateway';
        panel.append(title, subtitle);
        const fields = f.fields.length ? f.fields : Object.keys(item.values);
        fields.forEach((name, i) => {
            const list = f.shortcutType.startsWith('GATHER_LIST') && i === 0;
            panel.append(field(name, item.values[name], v => update(kind, index, name, v), list ? 'Plusieurs valeurs : séparées par des virgules' : null));
        });
        if (!fields.length) {
            const none = document.createElement('p');
            none.className = 'hint';
            none.textContent = 'Aucun argument.';
            panel.append(none);
        }
        const actions = document.createElement('div');
        actions.className = 'panel-actions';
        actions.append(button('← Avant', () => move(kind, index, index - 1)),
            button('Après →', () => move(kind, index, index + 1)),
            button('Supprimer', () => remove(kind, index), 'danger'));
        panel.append(actions);
    }

    function render() {
        state.renderer.render(state.route, state.selected);
        renderPanel();
        renderYaml();
    }

    return {
        state, load, blank, select, add, remove, move, render, shortcut, yaml,
        useRenderer(renderer) { state.renderer = renderer; }
    };
})();
