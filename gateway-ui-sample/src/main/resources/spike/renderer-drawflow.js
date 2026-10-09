/*
 * SPIKE #28 : moteur de rendu Drawflow (graphe libre : déplacement des nœuds, zoom, déplacement du plan).
 * Le pipeline d'une route étant linéaire, l'ordre des filtres est déduit de leur position horizontale.
 */
const DrawflowRenderer = (() => {
    const STEP_X = 250, BASE_Y = 140, STEP_Y = 110;
    let editor = null;
    let nodes = {};

    const escape = text => String(text).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
    const detail = (kind, item) => Editor.shortcut(kind, item).replace(/^[^=]*=?/, '');

    function html(title, args) {
        return `<div class="df-title">${escape(title)}</div><div class="df-args mono">${escape(args)}</div>`;
    }

    function add(kind, index, title, args, x, y, inputs, outputs, selected) {
        const id = editor.addNode(kind, inputs, outputs, x, y, 'df-' + kind + (selected ? ' df-selected' : ''), {}, html(title, args));
        nodes[id] = { kind, index };
        return id;
    }

    // Déplacer un filtre horizontalement change sa place dans la chaîne
    function reorder(movedId) {
        const moved = nodes[movedId];
        if (!moved || moved.kind !== 'filter') {
            return;
        }
        const filters = Object.entries(nodes).filter(([, n]) => n.kind === 'filter')
            .map(([id, n]) => ({ index: n.index, x: editor.getNodeFromId(id).pos_x }))
            .sort((a, b) => a.x - b.x);
        const target = filters.findIndex(f => f.index === moved.index);
        if (target !== moved.index) {
            Editor.move('filter', moved.index, target);
        } else {
            Editor.render();
        }
    }

    function render(route, selected) {
        const canvas = document.getElementById('canvas');
        canvas.replaceChildren();
        const host = document.createElement('div');
        host.className = 'drawflow-host';
        canvas.append(host);
        editor = new Drawflow(host);
        editor.reroute = false;
        editor.start();
        nodes = {};

        const request = add('request', 0, 'Requête entrante', '', 20, BASE_Y, 0, 1, false);
        const top = BASE_Y - (route.predicates.length - 1) * STEP_Y / 2;
        const predicateIds = route.predicates.map((p, i) => add('predicate', i, p.name, detail('predicate', p),
            20 + STEP_X, top + i * STEP_Y, 1, 1, selected.kind === 'predicate' && selected.index === i));
        const filterIds = route.filters.map((f, i) => add('filter', i, (i + 1) + '. ' + f.name, detail('filter', f),
            20 + (i + 2) * STEP_X, BASE_Y, 1, 1, selected.kind === 'filter' && selected.index === i));
        const target = add('target', 0, 'Cible', route.uri, 20 + (route.filters.length + 2) * STEP_X, BASE_Y, 1, 0,
            selected.kind === 'target');

        // Prédicats en parallèle (tous doivent correspondre), puis filtres en série jusqu'à la cible
        const afterPredicates = filterIds.length ? filterIds[0] : target;
        predicateIds.forEach(id => {
            editor.addConnection(request, id, 'output_1', 'input_1');
            editor.addConnection(id, afterPredicates, 'output_1', 'input_1');
        });
        [...filterIds, target].forEach((id, i, chain) => {
            if (i + 1 < chain.length) {
                editor.addConnection(id, chain[i + 1], 'output_1', 'input_1');
            }
        });

        // Zoom ajusté pour que toute la chaîne tienne dans la largeur du canevas
        // Drawflow zoome autour du centre : on décale le plan pour garder la chaîne calée à gauche
        const width = 40 + (route.filters.length + 3) * STEP_X;
        const scale = Math.max(editor.zoom_min, Math.min(1, host.clientWidth / width));
        editor.zoom = scale;
        editor.zoom_last_value = scale;
        editor.canvas_x = -(1 - scale) * host.clientWidth / 2;
        editor.canvas_y = 0;
        editor.precanvas.style.transform = `translate(${editor.canvas_x}px, ${editor.canvas_y}px) scale(${scale})`;

        editor.on('nodeSelected', id => {
            const node = nodes[id];
            if (node && node.kind !== 'request') {
                Editor.select(node.kind, node.index);
            }
        });
        editor.on('nodeMoved', reorder);
        // Le pipeline est imposé : une liaison ajoutée ou retirée à la main est annulée
        editor.on('connectionCreated', () => setTimeout(Editor.render));
        editor.on('connectionRemoved', () => setTimeout(Editor.render));
        editor.on('nodeRemoved', id => {
            const node = nodes[id];
            setTimeout(() => (node && node.kind !== 'request' && node.kind !== 'target')
                ? Editor.remove(node.kind, node.index) : Editor.render());
        });
    }

    function refreshLabels(route) {
        Object.entries(nodes).forEach(([id, node]) => {
            const element = document.querySelector(`#node-${id} .df-args`);
            if (!element) {
                return;
            }
            if (node.kind === 'target') {
                element.textContent = route.uri;
            } else if (node.kind !== 'request') {
                const item = (node.kind === 'predicate' ? route.predicates : route.filters)[node.index];
                element.textContent = detail(node.kind, item);
            }
        });
    }

    return {
        title: 'Drawflow',
        hint: 'Cliquez sur un nœud pour l\'éditer. Glissez un filtre horizontalement pour changer sa place ; molette + Ctrl pour zoomer, glisser le fond pour se déplacer.',
        render, refreshLabels
    };
})();
