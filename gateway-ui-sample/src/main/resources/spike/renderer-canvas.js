/*
 * SPIKE #28 : moteur de rendu « canevas maison » (HTML/CSS pour les nœuds, SVG pour les liaisons, glisser-déposer natif).
 */
const CanvasRenderer = (() => {
    const SVG = 'http://www.w3.org/2000/svg';

    function card(kind, index, title, detail, selected) {
        const element = document.createElement('div');
        element.className = 'node node-' + kind + (selected ? ' node-selected' : '');
        element.dataset.kind = kind;
        element.dataset.index = index;
        const name = document.createElement('span');
        name.className = 'node-name';
        name.textContent = title;
        const args = document.createElement('span');
        args.className = 'node-args mono';
        args.textContent = detail;
        element.append(name, args);
        if (kind !== 'request') {
            element.addEventListener('click', () => Editor.select(kind, index));
        }
        return element;
    }

    const detail = (kind, item) => Editor.shortcut(kind, item).replace(/^[^=]*=?/, '');

    function draggable(element, index) {
        element.draggable = true;
        element.addEventListener('dragstart', event => {
            event.dataTransfer.setData('text/plain', String(index));
            element.classList.add('node-dragging');
        });
        element.addEventListener('dragend', () => element.classList.remove('node-dragging'));
        element.addEventListener('dragover', event => {
            event.preventDefault();
            element.classList.add('node-drop');
        });
        element.addEventListener('dragleave', () => element.classList.remove('node-drop'));
        element.addEventListener('drop', event => {
            event.preventDefault();
            Editor.move('filter', Number(event.dataTransfer.getData('text/plain')), index);
        });
    }

    // Liaison entre deux nœuds : courbe sur une même ligne, coude quand la chaîne passe à la ligne suivante
    function link(svg, origin, from, to) {
        const a = from.getBoundingClientRect();
        const b = to.getBoundingClientRect();
        const path = document.createElementNS(SVG, 'path');
        if (b.top >= a.bottom) {
            const x1 = a.left + a.width / 2 - origin.left, y1 = a.bottom - origin.top;
            const x2 = b.left + b.width / 2 - origin.left, y2 = b.top - origin.top;
            const middle = (y1 + y2) / 2;
            path.setAttribute('d', `M${x1},${y1} V${middle} H${x2} V${y2}`);
        } else {
            const x1 = a.right - origin.left, y1 = a.top + a.height / 2 - origin.top;
            const x2 = b.left - origin.left, y2 = b.top + b.height / 2 - origin.top;
            const bend = Math.max(30, Math.abs(x2 - x1) / 2);
            path.setAttribute('d', `M${x1},${y1} C${x1 + bend},${y1} ${x2 - bend},${y2} ${x2},${y2}`);
        }
        path.setAttribute('class', 'link');
        svg.append(path);
    }

    function render(route, selected) {
        const canvas = document.getElementById('canvas');
        canvas.replaceChildren();
        const flow = document.createElement('div');
        flow.className = 'canvas-flow';

        const steps = [card('request', 0, 'Requête entrante', '', false)];
        const predicates = document.createElement('div');
        predicates.className = 'node-group';
        const label = document.createElement('span');
        label.className = 'group-label';
        label.textContent = 'Prédicats (tous)';
        predicates.append(label);
        route.predicates.forEach((p, i) => predicates.append(
            card('predicate', i, p.name, detail('predicate', p), selected.kind === 'predicate' && selected.index === i)));
        steps.push(predicates);
        route.filters.forEach((f, i) => {
            const element = card('filter', i, (i + 1) + '. ' + f.name, detail('filter', f), selected.kind === 'filter' && selected.index === i);
            draggable(element, i);
            steps.push(element);
        });
        steps.push(card('target', 0, 'Cible', route.uri, selected.kind === 'target'));
        flow.append(...steps);

        const svg = document.createElementNS(SVG, 'svg');
        svg.setAttribute('class', 'canvas-links');
        canvas.append(svg, flow);
        const origin = canvas.getBoundingClientRect();
        for (let i = 0; i + 1 < steps.length; i++) {
            link(svg, origin, steps[i], steps[i + 1]);
        }
    }

    // Mise à jour des libellés sans tout redessiner, pour ne pas perdre la saisie en cours
    function refreshLabels(route) {
        document.querySelectorAll('#canvas .node').forEach(element => {
            const { kind, index } = element.dataset;
            const args = element.querySelector('.node-args');
            if (kind === 'predicate' || kind === 'filter') {
                const item = (kind === 'predicate' ? route.predicates : route.filters)[Number(index)];
                args.textContent = detail(kind, item);
            } else if (kind === 'target') {
                args.textContent = route.uri;
            }
        });
    }

    return {
        title: 'Canevas maison',
        hint: 'Cliquez sur un nœud pour l\'éditer. Glissez un filtre sur un autre pour le déplacer dans la chaîne.',
        render, refreshLabels
    };
})();
