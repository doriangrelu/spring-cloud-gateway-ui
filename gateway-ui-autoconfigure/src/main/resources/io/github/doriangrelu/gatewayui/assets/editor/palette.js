/*
 * Éditeur graphique : palette de recherche pour ajouter un prédicat ou un filtre, pilotable au clavier
 * (combobox ARIA : la liste est annoncée par aria-activedescendant, le focus reste dans le champ).
 */
import { Catalog } from './catalog.js';
import { Model } from './model.js';
import { t } from './i18n.js';

const normalize = text => text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
const dialog = document.getElementById('palette');
const input = document.getElementById('palette-input');
const list = document.getElementById('palette-list');
let pick = null;
let kind = 'filter';
let results = [];
let active = 0;

// Sans recherche, les fabriques documentées d'abord ; sinon le nom prime sur le résumé et la catégorie
function score(name, query) {
    const entry = Catalog.get(kind, name);
    const normalized = normalize(name);
    if (!query) {
        return entry && entry.documented ? 0 : 1;
    }
    if (normalized.startsWith(query)) {
        return 0;
    }
    if (normalized.includes(query)) {
        return 1;
    }
    return entry && normalize(entry.summary + ' ' + entry.category).includes(query) ? 2 : -1;
}

function option(name, index) {
    const element = document.createElement('li');
    element.id = 'palette-option-' + index;
    element.setAttribute('role', 'option');
    element.setAttribute('aria-selected', String(index === active));
    element.className = index === active ? 'active' : '';
    const head = document.createElement('div');
    head.className = 'palette-head';
    const title = document.createElement('strong');
    title.textContent = name;
    head.append(title);
    const entry = Catalog.get(kind, name);
    if (entry) {
        const category = document.createElement('span');
        category.className = 'tag';
        category.textContent = entry.category;
        head.append(category);
    }
    const fields = Model.factory(kind, name).fields;
    const summary = document.createElement('div');
    summary.className = 'palette-summary' + (entry && entry.documented ? '' : ' muted');
    summary.textContent = entry ? entry.summary : (fields.length ? fields.join(', ') : t('palette.noArgs'));
    element.append(head, summary);
    element.addEventListener('mousedown', event => {
        event.preventDefault();
        choose(index);
    });
    return element;
}

function render() {
    list.replaceChildren(...results.map(option));
    if (!results.length) {
        const empty = document.createElement('li');
        empty.className = 'muted';
        empty.textContent = t('palette.empty');
        list.append(empty);
    }
    input.setAttribute('aria-activedescendant', results.length ? 'palette-option-' + active : '');
    document.getElementById('palette-option-' + active)?.scrollIntoView({ block: 'nearest' });
}

function search(query) {
    const q = normalize(query.trim());
    results = Model.names(kind)
        .map(name => ({ name, rank: score(name, q) }))
        .filter(result => result.rank >= 0)
        .sort((a, b) => a.rank - b.rank || a.name.localeCompare(b.name))
        .map(result => result.name);
    active = 0;
    render();
}

function choose(index) {
    const name = results[index];
    dialog.close();
    if (name && pick) {
        pick(name);
    }
}

input.addEventListener('input', () => search(input.value));
input.addEventListener('keydown', event => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        active = (active + (event.key === 'ArrowDown' ? 1 : -1) + results.length) % Math.max(results.length, 1);
        render();
    } else if (event.key === 'Enter') {
        event.preventDefault();
        choose(active);
    }
});

export const Palette = {
    open(target, onPick) {
        kind = target;
        pick = onPick;
        document.getElementById('palette-title').textContent = t(kind === 'predicate' ? 'palette.predicate' : 'palette.filter');
        input.value = '';
        dialog.showModal();
        search('');
        input.focus();
    },
    isOpen: () => dialog.open
};
