/*
 * SPIKE #28 : palette de recherche pour ajouter un prédicat ou un filtre (nom, description, catégorie), pilotable au clavier.
 */
const Palette = (() => {
    const normalize = text => text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    let pick = null;
    let results = [];
    let active = 0;

    function score(item, query) {
        if (!query) {
            return item.documented ? 1 : 2;
        }
        const name = normalize(item.name);
        if (name.startsWith(query)) {
            return 0;
        }
        if (name.includes(query)) {
            return 1;
        }
        return normalize(item.summary + ' ' + item.category).includes(query) ? 2 : -1;
    }

    function render() {
        const list = document.getElementById('palette-list');
        list.replaceChildren(...results.map((item, index) => {
            const option = document.createElement('li');
            option.id = 'palette-option-' + index;
            option.setAttribute('role', 'option');
            option.setAttribute('aria-selected', String(index === active));
            option.className = index === active ? 'active' : '';
            const head = document.createElement('div');
            head.className = 'palette-head';
            const name = document.createElement('strong');
            name.textContent = item.name;
            const category = document.createElement('span');
            category.className = 'tag';
            category.textContent = item.category;
            head.append(name, category);
            const summary = document.createElement('div');
            summary.className = 'palette-summary' + (item.documented ? '' : ' muted');
            summary.textContent = item.summary;
            option.append(head, summary);
            option.addEventListener('mousedown', event => { event.preventDefault(); choose(index); });
            return option;
        }));
        document.getElementById('palette-input').setAttribute('aria-activedescendant', results.length ? 'palette-option-' + active : '');
        const current = document.getElementById('palette-option-' + active);
        if (current) {
            current.scrollIntoView({ block: 'nearest' });
        }
    }

    function search(kind, query) {
        const names = (kind === 'predicate' ? Model.factories().predicates : Model.factories().filters).map(f => f.name);
        const q = normalize(query.trim());
        results = names.map(name => Catalog.describe(kind, name))
            .map(item => ({ item, rank: score(item, q) }))
            .filter(r => r.rank >= 0)
            .sort((a, b) => a.rank - b.rank || a.item.name.localeCompare(b.item.name))
            .map(r => r.item);
        active = 0;
        render();
    }

    function choose(index) {
        const item = results[index];
        document.getElementById('palette').close();
        if (item && pick) {
            pick(item.name);
        }
    }

    function open(kind, onPick) {
        pick = onPick;
        const dialog = document.getElementById('palette');
        const input = document.getElementById('palette-input');
        document.getElementById('palette-title').textContent = kind === 'predicate' ? 'Ajouter un prédicat' : 'Ajouter un filtre';
        input.value = '';
        input.oninput = () => search(kind, input.value);
        input.onkeydown = event => {
            if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                event.preventDefault();
                active = (active + (event.key === 'ArrowDown' ? 1 : -1) + results.length) % Math.max(results.length, 1);
                render();
            } else if (event.key === 'Enter') {
                event.preventDefault();
                choose(active);
            }
        };
        dialog.showModal();
        search(kind, '');
        input.focus();
    }

    return { open };
})();
