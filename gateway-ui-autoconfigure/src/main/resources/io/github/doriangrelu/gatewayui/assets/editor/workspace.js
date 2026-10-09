/*
 * Éditeur graphique : espace de travail (ADR 0012). Toutes les routes de la configuration en cours d'édition, avec leur
 * statut : inchangée, modifiée, nouvelle, retirée, ou Java (lecture seule). Conservé dans le navigateur uniquement.
 */
import { Model } from './model.js';
import { t } from './i18n.js';

const STORAGE = 'gateway-ui-editor-workspace';
let entries = [];
let selectedKey = null;
let restored = false;

const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
const find = key => entries.find(entry => entry.key === key);

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
        const originals = gatewayEntries.filter(entry => !entry.java).map(entry => entry.original);
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
        const originals = entries.filter(entry => entry.original).map(entry => entry.original);
        localStorage.setItem(STORAGE, JSON.stringify({ originals, entries, selectedKey }));
    } catch (error) {
        // Stockage indisponible : le travail reste en mémoire le temps de la visite
    }
}

function uniqueId(base) {
    let id = base;
    for (let i = 2; entries.some(entry => entry.current && entry.current.id === id); i++) {
        id = base + '-' + i;
    }
    return id;
}

function add(route) {
    const key = 'new:' + Date.now();
    entries.push({ key, original: null, current: route, removed: false, java: false });
    selectedKey = key;
    save();
    return key;
}

export const Workspace = {
    init(routes, javaIds) {
        const gatewayEntries = [
            ...routes.map(route => ({ key: 'gw:' + route.id, original: Model.copy(route), current: route, removed: false, java: false })),
            ...javaIds.map(id => ({ key: 'java:' + id, id, original: null, current: null, removed: false, java: true }))
        ];
        restored = restore(gatewayEntries);
        if (!restored) {
            entries = gatewayEntries;
            selectedKey = entries.length ? entries[0].key : null;
        }
    },

    save,
    status,
    find,
    wasRestored: () => restored,
    entries: () => entries,
    selected: () => find(selectedKey),

    select(key) {
        selectedKey = key;
        save();
    },

    // Routes éditables autres que la route sélectionnée, pour les conseils (identifiant en double...)
    others() {
        return entries.filter(entry => entry.key !== selectedKey && entry.current && !entry.removed).map(entry => entry.current);
    },

    create() {
        return add({ id: uniqueId(t('newRouteId')), uri: 'http://service.namespace.svc.cluster.local:8080', order: 0,
            predicates: [{ name: 'Path', values: { patterns: '/api/**' } }], filters: [] });
    },

    duplicate(key) {
        const source = find(key).current;
        return add({ ...Model.copy(source), id: uniqueId(source.id + '-' + t('copySuffix')) });
    },

    remove(key) {
        const entry = find(key);
        if (entry.original) {
            entry.removed = true;
        } else {
            entries = entries.filter(other => other !== entry);
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

    clear() {
        try {
            localStorage.removeItem(STORAGE);
        } catch (error) {
            // Rien à effacer
        }
    },

    // Contenu des trois exports : route courante, configuration complète, modifications seulement
    exported(mode) {
        const warning = t('export.secrets');
        if (mode === 'current') {
            const entry = find(selectedKey);
            return entry && entry.current && !entry.removed ? { routes: [entry.current], comments: [warning] } : null;
        }
        if (mode === 'full') {
            const routes = entries.filter(entry => !entry.java && !entry.removed).map(entry => entry.current);
            return { routes, comments: [t('export.full.comment'), warning] };
        }
        const changed = entries.filter(entry => ['new', 'modified'].includes(status(entry))).map(entry => entry.current);
        const removed = entries.filter(entry => status(entry) === 'removed').map(entry => t('export.removed', entry.original.id));
        return { routes: changed, comments: [t('export.changes.comment'), ...removed, warning] };
    }
};
