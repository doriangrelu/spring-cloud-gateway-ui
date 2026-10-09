/*
 * SPIKE #28 : modèle d'une route (arguments rattachés aux champs des fabriques) et génération du YAML.
 */
const Model = (() => {
    let factories = { predicates: [], filters: [] };

    const factory = (kind, name) => (kind === 'predicate' ? factories.predicates : factories.filters)
        .find(f => f.name === name) || { name, fields: [], shortcutType: 'DEFAULT' };

    // Les arguments déclarés en forme raccourcie arrivent indexés (_genkey_0...) : on les rattache aux champs de la fabrique
    function fromDefinition(kind, definition) {
        const f = factory(kind, definition.name);
        const raw = Object.entries(definition.args || {});
        const values = {};
        if (!raw.every(([key]) => key.startsWith('_genkey_'))) {
            raw.forEach(([key, value]) => { values[key] = value; });
        } else if (f.shortcutType.startsWith('GATHER_LIST')) {
            const items = raw.map(([, value]) => value);
            if (f.shortcutType === 'GATHER_LIST_TAIL_FLAG' && /^(true|false)$/.test(items[items.length - 1] || '')) {
                values[f.fields[1]] = items.pop();
            }
            values[f.fields[0]] = items.join(', ');
        } else {
            raw.forEach(([, value], index) => { values[f.fields[index] || ('arg' + index)] = value; });
        }
        return { name: definition.name, values };
    }

    function route(definition) {
        return {
            id: definition.id, uri: definition.uri, order: String(definition.order || 0),
            predicates: definition.predicates.map(p => fromDefinition('predicate', p)),
            filters: definition.filters.map(f => fromDefinition('filter', f))
        };
    }

    const fields = (kind, item) => {
        const f = factory(kind, item.name);
        return f.fields.length ? f.fields : Object.keys(item.values);
    };

    // Forme raccourcie : Nom=valeur1, valeur2
    function shortcut(kind, item) {
        const values = fields(kind, item).map(field => (item.values[field] || '').trim());
        while (values.length && values[values.length - 1] === '') {
            values.pop();
        }
        return values.length ? item.name + '=' + values.join(', ') : item.name;
    }

    // Spring résout ${...} dans le YAML : un $ { littéral (RewritePath) doit s'écrire $\{
    function yamlValue(text) {
        const escaped = String(text).replace(/\$\{/g, '$\\{');
        return /(: | #|^[*&!|>'"%@`{}\[\]])/.test(escaped) ? "'" + escaped.replace(/'/g, "''") + "'" : escaped;
    }

    function routeYaml(r) {
        const lines = ['            - id: ' + yamlValue(r.id), '              uri: ' + yamlValue(r.uri)];
        if (r.order !== '0' && r.order !== '') {
            lines.push('              order: ' + r.order);
        }
        [['predicates', 'predicate'], ['filters', 'filter']].forEach(([key, kind]) => {
            if (r[key].length) {
                lines.push('              ' + key + ':');
                r[key].forEach(item => lines.push('                - ' + yamlValue(shortcut(kind, item))));
            }
        });
        return lines;
    }

    function yaml(routes, comments) {
        const header = (comments || []).map(comment => '# ' + comment);
        const body = ['spring:', '  cloud:', '    gateway:', '      server:', '        webflux:', '          routes:'];
        routes.forEach(r => body.push(...routeYaml(r)));
        if (!routes.length) {
            body.push('            []');
        }
        return [...header, ...body].join('\n');
    }

    return {
        setFactories(value) { factories = value; },
        factories: () => factories,
        factory, fields, route, shortcut, yaml,
        copy: value => JSON.parse(JSON.stringify(value))
    };
})();
