/*
 * SPIKE #28 : espace de travail. Toutes les routes de la configuration en cours d'édition, avec leur statut :
 * inchangée, modifiée, nouvelle, retirée, ou Java (lecture seule). Conservé dans le navigateur (localStorage).
 */
const Workspace = (() => {
    const STORAGE = 'gateway-ui-editor-workspace';
    let entries = [];
    let selectedKey = null;
    let restored = false;

    const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);

    function status(entry) {
        if (entry.java) {
            return 'java';
        }
        if (!entry.original) {
            return 'new';
        }
        if (entry.removed) {
            return 'removed';
        }
        return same(entry.original, entry.current) ? 'unchanged' : 'modified';
    }

    // Un espace sauvegardé n'est repris que si la configuration de la Gateway n'a pas changé entre-temps
    function restore(gatewayEntries) {
        try {
            const saved = JSON.parse(localStorage.getItem(STORAGE) || 'null');
            const originals = gatewayEntries.filter(e => !e.java).map(e => e.original);
            if (saved && same(saved.originals, originals)) {
                entries = saved.entries;
                selectedKey = saved.selectedKey;
                return true;
            }
        } catch (error) {
            // Stockage indisponible ou corrompu : on repart de la configuration de la Gateway
        }
        return false;
    }

    function save() {
        try {
            const originals = entries.filter(e => e.original).map(e => e.original);
            localStorage.setItem(STORAGE, JSON.stringify({ originals, entries, selectedKey }));
        } catch (error) {
            // Stockage indisponible : le travail reste en mémoire pour la session
        }
    }

    function init(definitions, javaIds) {
        const gatewayEntries = [
            ...definitions.map(d => {
                const r = Model.route(d);
                return { key: 'gw:' + d.id, original: Model.copy(r), current: r, removed: false, java: false };
            }),
            ...javaIds.map(id => ({ key: 'java:' + id, id, original: null, current: null, removed: false, java: true }))
        ];
        restored = restore(gatewayEntries);
        if (!restored) {
            entries = gatewayEntries;
            selectedKey = entries.length ? entries[0].key : null;
        }
    }

    const find = key => entries.find(e => e.key === key);
    const uniqueId = base => {
        let id = base;
        for (let i = 2; entries.some(e => e.current && e.current.id === id); i++) {
            id = base + '-' + i;
        }
        return id;
    };

    function add(route) {
        const key = 'new:' + Date.now();
        entries.push({ key, original: null, current: route, removed: false, java: false });
        selectedKey = key;
        save();
        return key;
    }

    function exportable() {
        return entries.filter(e => !e.java && !e.removed).map(e => e.current);
    }

    return {
        init, save, status, find,
        wasRestored: () => restored,
        entries: () => entries,
        selected: () => find(selectedKey),
        select(key) { selectedKey = key; save(); },
        create() {
            return add({ id: uniqueId('nouvelle-route'), uri: 'http://service.namespace.svc.cluster.local:8080', order: '0',
                predicates: [{ name: 'Path', values: { patterns: '/api/**' } }], filters: [] });
        },
        duplicate(key) {
            const source = find(key).current;
            return add({ ...Model.copy(source), id: uniqueId(source.id + '-copie') });
        },
        remove(key) {
            const entry = find(key);
            if (entry.original) {
                entry.removed = true;
            } else {
                entries = entries.filter(e => e !== entry);
                selectedKey = entries.length ? entries[0].key : null;
            }
            save();
        },
        reset(key) {
            const entry = find(key);
            entry.current = Model.copy(entry.original);
            entry.removed = false;
            save();
        },
        resetAll() {
            try {
                localStorage.removeItem(STORAGE);
            } catch (error) {
                // Rien à effacer
            }
        },
        // Exports : route courante, configuration complète, ou modifications seulement
        yaml(mode) {
            const warning = 'Valeurs résolues par la Gateway : vérifiez placeholders ${...} et secrets avant de committer.';
            if (mode === 'current') {
                const entry = find(selectedKey);
                return entry && entry.current ? Model.yaml([entry.current], [warning]) : '# Aucune route éditable sélectionnée';
            }
            if (mode === 'full') {
                return Model.yaml(exportable(), ['Configuration complète : remplace tout le bloc routes', warning]);
            }
            const changed = entries.filter(e => ['new', 'modified'].includes(status(e))).map(e => e.current);
            const removed = entries.filter(e => status(e) === 'removed').map(e => 'Route retirée : ' + e.original.id);
            return Model.yaml(changed, ['Modifications seulement : routes nouvelles et modifiées', ...removed, warning]);
        }
    };
})();
