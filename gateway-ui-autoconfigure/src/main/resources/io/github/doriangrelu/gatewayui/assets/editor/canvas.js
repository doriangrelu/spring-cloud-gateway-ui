/*
 * Éditeur graphique : canevas maison (ADR 0011). Nœuds en HTML, liaisons en SVG, glisser-déposer natif pour réordonner
 * les filtres. Chaque nœud est un bouton : il s'atteint et s'active au clavier.
 */
import { Model } from './model.js';
import { t } from './i18n.js';

const SVG = 'http://www.w3.org/2000/svg';
const canvas = document.getElementById('canvas');

function node(kind, index, title, detail, selected, onSelect) {
    const element = document.createElement(kind === 'request' ? 'div' : 'button');
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
        element.type = 'button';
        element.setAttribute('aria-pressed', String(selected));
        element.addEventListener('click', () => onSelect(kind, index));
    }
    return element;
}

function draggable(element, index, onMove) {
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
        onMove(Number(event.dataTransfer.getData('text/plain')), index);
    });
}

// Liaison entre deux nœuds : courbe sur une même ligne, coude quand la chaîne passe à la ligne suivante
function link(svg, origin, from, to) {
    const a = from.getBoundingClientRect();
    const b = to.getBoundingClientRect();
    const path = document.createElementNS(SVG, 'path');
    if (b.top >= a.bottom) {
        const x1 = a.left + a.width / 2 - origin.left;
        const y1 = a.bottom - origin.top;
        const x2 = b.left + b.width / 2 - origin.left;
        const y2 = b.top - origin.top;
        const middle = (y1 + y2) / 2;
        path.setAttribute('d', `M${x1},${y1} V${middle} H${x2} V${y2}`);
    } else {
        const x1 = a.right - origin.left;
        const y1 = a.top + a.height / 2 - origin.top;
        const x2 = b.left - origin.left;
        const y2 = b.top + b.height / 2 - origin.top;
        const bend = Math.max(30, Math.abs(x2 - x1) / 2);
        path.setAttribute('d', `M${x1},${y1} C${x1 + bend},${y1} ${x2 - bend},${y2} ${x2},${y2}`);
    }
    path.setAttribute('class', 'link');
    svg.append(path);
}

function predicates(route, selection, onSelect) {
    const group = document.createElement('div');
    group.className = 'node-group';
    const label = document.createElement('span');
    label.className = 'group-label';
    label.textContent = t('canvas.predicates');
    group.append(label);
    route.predicates.forEach((predicate, i) => group.append(node('predicate', i, predicate.name, Model.summary('predicate', predicate),
        selection.kind === 'predicate' && selection.index === i, onSelect)));
    if (!route.predicates.length) {
        const empty = document.createElement('span');
        empty.className = 'muted';
        empty.textContent = t('canvas.noPredicate');
        group.append(empty);
    }
    return group;
}

export const Canvas = {
    render(route, selection, { onSelect, onMove }) {
        canvas.replaceChildren();
        const flow = document.createElement('div');
        flow.className = 'canvas-flow';
        const steps = [node('request', 0, t('canvas.request'), '', false), predicates(route, selection, onSelect)];
        route.filters.forEach((filter, i) => {
            const element = node('filter', i, (i + 1) + '. ' + filter.name, Model.summary('filter', filter),
                selection.kind === 'filter' && selection.index === i, onSelect);
            draggable(element, i, onMove);
            steps.push(element);
        });
        steps.push(node('target', 0, t('canvas.target'), route.uri, selection.kind === 'target', onSelect));
        flow.append(...steps);

        const svg = document.createElementNS(SVG, 'svg');
        svg.setAttribute('class', 'canvas-links');
        svg.setAttribute('aria-hidden', 'true');
        canvas.append(svg, flow);
        const origin = canvas.getBoundingClientRect();
        for (let i = 0; i + 1 < steps.length; i++) {
            link(svg, origin, steps[i], steps[i + 1]);
        }
    },

    message(text) {
        const paragraph = document.createElement('p');
        paragraph.className = 'empty';
        paragraph.textContent = text;
        canvas.replaceChildren(paragraph);
    },

    // Mise à jour des libellés sans tout redessiner, pour ne pas perdre la saisie en cours
    refreshLabels(route) {
        canvas.querySelectorAll('.node').forEach(element => {
            const { kind, index } = element.dataset;
            const args = element.querySelector('.node-args');
            if (kind === 'predicate' || kind === 'filter') {
                const item = (kind === 'predicate' ? route.predicates : route.filters)[Number(index)];
                args.textContent = Model.summary(kind, item);
            } else if (kind === 'target') {
                args.textContent = route.uri;
            }
        });
    }
};
