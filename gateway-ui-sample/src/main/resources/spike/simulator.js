/*
 * SPIKE #28 : test en direct de la route éditée et conseils.
 * Simulation côté navigateur, approchée, pour juger de l'ergonomie. La version finale réutiliserait le simulateur du
 * serveur (testeur), qui reproduit exactement la Gateway.
 */
const Simulator = (() => {
    const escapeRegex = text => text.replace(/[.+^$()|[\]\\]/g, '\\$&');

    // Motif PathPattern de Spring vers expression régulière : ** , * , ? , {var} , {*var} , {var:regex}
    function pathRegex(pattern) {
        let source = '';
        for (let i = 0; i < pattern.length;) {
            const rest = pattern.slice(i);
            let match;
            if (rest === '/**') {
                source += '(?:/.*)?'; i += 3;
            } else if (rest.startsWith('**')) {
                source += '.*'; i += 2;
            } else if ((match = rest.match(/^\{\*(\w+)\}/))) {
                source += `(?<${match[1]}>(?:/.*)?)`; i += match[0].length;
            } else if ((match = rest.match(/^\{(\w+):([^}]+)\}/))) {
                source += `(?<${match[1]}>${match[2]})`; i += match[0].length;
            } else if ((match = rest.match(/^\{(\w+)\}/))) {
                source += `(?<${match[1]}>[^/]+)`; i += match[0].length;
            } else if (rest[0] === '*') {
                source += '[^/]*'; i += 1;
            } else if (rest[0] === '?') {
                source += '[^/]'; i += 1;
            } else {
                source += escapeRegex(rest[0]); i += 1;
            }
        }
        return new RegExp('^' + source + '/?$');
    }

    const list = value => (value || '').split(',').map(v => v.trim()).filter(Boolean);
    const expand = (template, vars) => template.replace(/\{(\w+)\}/g, (all, name) => (name in vars ? vars[name] : all));

    function matchPredicates(route, path, method) {
        const vars = {};
        for (const p of route.predicates) {
            if (p.name === 'Path') {
                const found = list(p.values.patterns).map(pattern => path.match(pathRegex(pattern))).find(Boolean);
                if (!found) {
                    return { matched: false, reason: 'Path=' + p.values.patterns };
                }
                Object.assign(vars, found.groups || {});
            } else if (p.name === 'Method' && !list(p.values.methods).map(m => m.toUpperCase()).includes(method)) {
                return { matched: false, reason: 'Method=' + p.values.methods };
            }
        }
        return { matched: true, vars };
    }

    const effects = {
        StripPrefix: (v, path) => '/' + path.split('/').filter(Boolean).slice(Number(v.parts || 1)).join('/'),
        PrefixPath: (v, path, vars) => expand(v.prefix || '', vars) + path,
        SetPath: (v, path, vars) => expand(v.template || '', vars),
        // Remplacement Java ($\{nom}, ${nom}, $1) vers JavaScript ($<nom>, $1)
        RewritePath: (v, path) => path.replace(new RegExp(v.regexp, 'g'),
            (v.replacement || '').replace(/\$\\?\{(\w+)\}/g, '$<$1>'))
    };

    function test(route, path, method) {
        const verdict = matchPredicates(route, path, method || 'GET');
        if (!verdict.matched) {
            return verdict;
        }
        let current = path;
        const steps = route.filters.map(f => {
            const effect = effects[f.name];
            if (!effect) {
                return { name: f.name, simulated: false };
            }
            const before = current;
            try {
                current = effect(f.values, current, verdict.vars);
            } catch (error) {
                return { name: f.name, simulated: false, error: error.message };
            }
            return { name: f.name, simulated: true, before, after: current };
        });
        const target = route.uri.replace(/\/$/, '').replace(/^(\w+:\/\/[^/]+).*$/, '$1') + current;
        return { matched: true, vars: verdict.vars, steps, target };
    }

    const SECRET_NAME = /authorization|token|secret|password|api[-_]?key/i;
    const SECRET_VALUE = /^(bearer|basic)\s|^[A-Za-z0-9+/_-]{32,}={0,2}$/i;

    function lint(route, others) {
        const advice = [];
        if (!route.id.trim()) {
            advice.push({ level: 'error', text: 'Identifiant obligatoire.' });
        } else if (others.some(r => r.id === route.id)) {
            advice.push({ level: 'error', text: `Identifiant « ${route.id} » déjà utilisé par une autre route.` });
        }
        if (!/^(https?|wss?|lb|forward|no):/.test(route.uri.trim())) {
            advice.push({ level: 'error', text: 'URI cible invalide : http://, https://, lb://, forward: ou no:// attendu.' });
        }
        [['predicate', route.predicates], ['filter', route.filters]].forEach(([kind, list]) => list.forEach(item => {
            const fields = Model.fields(kind, item);
            if (fields.length && !(item.values[fields[0]] || '').trim()) {
                advice.push({ level: 'error', text: `${item.name} : argument « ${fields[0]} » à renseigner.` });
            }
        }));
        if (!route.predicates.length) {
            advice.push({ level: 'warn', text: 'Sans prédicat, cette route reçoit toutes les requêtes.' });
        }
        route.predicates.filter(p => p.name === 'Path' && list(p.values.patterns).some(v => v === '/**'))
            .forEach(() => advice.push({ level: 'warn', text: 'Path=/** intercepte tout : les routes évaluées après celle-ci seront masquées. Donnez-lui un ordre élevé (order).' }));
        route.filters.forEach(f => Object.entries(f.values).forEach(([field, value]) => {
            const named = SECRET_NAME.test(f.values.name || '') || SECRET_NAME.test(field);
            if (value && !value.includes('${') && (named || SECRET_VALUE.test(value))) {
                advice.push({ level: 'warn', text: `${f.name} : « ${field} » ressemble à un secret. Préférez un placeholder \${...} plutôt qu'une valeur en clair.` });
            }
        }));
        route.filters.filter(f => f.name === 'RewritePath').forEach(f => {
            try {
                new RegExp(f.values.regexp);
            } catch (error) {
                advice.push({ level: 'error', text: 'RewritePath : expression régulière invalide.' });
            }
        });
        return advice;
    }

    return { test, lint };
})();
