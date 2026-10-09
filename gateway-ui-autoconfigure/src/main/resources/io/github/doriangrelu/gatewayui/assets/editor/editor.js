/*
 * Éditeur graphique : orchestration (liste des routes, contexte, canevas, panneau des propriétés, test en direct,
 * conseils, export). Les résultats viennent de l'API : ce module ne calcule rien lui-même (ADR 0011).
 */
import { Api } from './api.js';
import { Canvas } from './canvas.js';
import { Catalog } from './catalog.js';
import { t } from './i18n.js';
import { Model } from './model.js';
import { Palette } from './palette.js';
import { Workspace } from './workspace.js';

const DELAY = 300;
const METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'];
const STATUS_CHIPS = { unchanged: '', modified: 'chip-shadowed', new: 'chip-match', removed: 'chip-error', java: '' };

let selection = { kind: 'target', index: 0 };
let exportMode = 'current';
let test = { method: 'GET', path: null };
let timer = null;
let sequence = 0;

const el = id => document.getElementById(id);

function make(tag, className, text) {
    const element = document.createElement(tag);
    if (className) {
        element.className = className;
    }
    if (text != null) {
        element.textContent = text;
    }
    return element;
}

function button(label, onClick, className) {
    const element = make('button', className || 'secondary', label);
    element.type = 'button';
    element.addEventListener('click', onClick);
    return element;
}

const entry = () => Workspace.selected();
const route = () => (entry() && !entry().java && !entry().removed ? entry().current : null);
const items = kind => (kind === 'predicate' ? route().predicates : route().filters);

function open(key) {
    Workspace.select(key);
    selection = { kind: 'target', index: 0 };
    test.path = null;
    render();
}

// Liste des routes : celles de la Gateway, puis les nouvelles
function renderSidebar() {
    const sidebar = el('sidebar');
    sidebar.replaceChildren();
    const groups = [['sidebar.gateway', e => e.original || e.java], ['sidebar.new', e => !e.original && !e.java]];
    groups.forEach(([title, filter]) => {
        sidebar.append(make('h3', 'side-title', t(title)));
        const list = make('ul', 'side-list');
        Workspace.entries().filter(filter).forEach(e => {
            const status = Workspace.status(e);
            const item = make('li', e === entry() ? 'active' : '');
            const link = button('', () => open(e.key), 'side-item');
            if (e === entry()) {
                link.setAttribute('aria-current', 'true');
            }
            link.append(make('span', 'mono', e.java ? e.id : e.current.id), make('span', 'chip ' + STATUS_CHIPS[status], t('status.' + status)));
            item.append(link);
            list.append(item);
        });
        sidebar.append(list);
    });
    sidebar.append(button(t('route.create'), () => open(Workspace.create()), 'side-new'));
}

function contextActions(e, status) {
    const actions = make('div', 'context-actions');
    if (route()) {
        actions.append(button(t('route.addPredicate'), () => Palette.open('predicate', name => add('predicate', name))),
            button(t('route.addFilter'), () => Palette.open('filter', name => add('filter', name)), 'primary add-filter'),
            button(t('route.duplicate'), () => open(Workspace.duplicate(e.key))));
    }
    if (status === 'modified' || status === 'removed') {
        actions.append(button(t(status === 'removed' ? 'route.restore' : 'route.reset'), () => { Workspace.reset(e.key); render(); }));
    }
    if (!e.java && status !== 'removed') {
        actions.append(button(t(e.original ? 'route.remove' : 'route.delete'), () => {
            Workspace.remove(e.key);
            selection = { kind: 'target', index: 0 };
            render();
        }, 'danger'));
    }
    return actions;
}

function renderContext() {
    const e = entry();
    const context = el('context');
    context.replaceChildren();
    if (!e) {
        return;
    }
    const status = Workspace.status(e);
    const title = make('div', 'context-title');
    title.append(make('span', 'context-label', t('context.' + status)), make('h2', 'mono', e.java ? e.id : e.current.id));
    context.append(title, contextActions(e, status));
}

function renderCanvas() {
    const e = entry();
    if (route()) {
        Canvas.render(route(), selection, { onSelect: select, onMove: (from, to) => move('filter', from, to) });
        return;
    }
    Canvas.message(!e ? t('canvas.empty') : t(e.java ? 'canvas.java' : 'canvas.removed'));
}

function input(label, value, onInput, hint) {
    const wrapper = make('label', 'panel-field', label);
    const field = make('input', 'mono');
    field.value = value == null ? '' : value;
    field.addEventListener('input', () => onInput(field.value, field));
    wrapper.append(field);
    if (hint) {
        wrapper.append(make('small', '', hint));
    }
    return wrapper;
}

function updateOrder(value, field) {
    const valid = /^-?\d+$/.test(value.trim());
    field.setAttribute('aria-invalid', String(!valid));
    if (valid) {
        update('target', 0, 'order', Number(value.trim()));
    }
}

function renderTargetPanel(panel) {
    panel.append(make('h2', '', t('panel.route')), make('p', 'hint', t('panel.route.hint')),
        input(t('panel.id'), route().id, v => update('target', 0, 'id', v), t('panel.id.hint')),
        input(t('panel.uri'), route().uri, v => update('target', 0, 'uri', v), t('panel.uri.hint')),
        input(t('panel.order'), route().order, updateOrder, t('panel.order.hint')));
}

function link(text, href, external) {
    const element = make('a', 'help-link', text);
    element.href = href;
    if (external) {
        element.target = '_blank';
        element.rel = 'noopener noreferrer';
    }
    return element;
}

// Aide du catalogue (ADR 0013) : catégorie, résumé, détails, exemple et liens
function help(entry) {
    const box = make('div', 'help');
    const head = make('div', 'help-head');
    head.append(make('span', 'tag', entry.category));
    if (entry.custom || !entry.documented) {
        head.append(make('span', 'chip', t(entry.custom ? 'help.custom' : 'help.undocumented')));
    }
    box.append(head, make('p', 'help-summary', entry.summary));
    if (entry.details) {
        box.append(make('p', 'help-details', entry.details));
    }
    if (entry.example) {
        box.append(make('pre', 'help-example mono', entry.example));
    }
    const links = make('div', 'help-links');
    links.append(link(t('help.catalog'), Catalog.page(entry), false));
    if (entry.documentation) {
        links.append(link(t('help.documentation'), entry.documentation, true));
    }
    box.append(links);
    return box;
}

function renderStepPanel(panel) {
    const { kind, index } = selection;
    const item = items(kind)[index];
    panel.append(make('span', 'context-label', t(kind === 'predicate' ? 'panel.predicate' : 'panel.filter')), make('h2', '', item.name));
    const entry = Catalog.get(kind, item.name);
    if (entry) {
        panel.append(help(entry));
    }
    const fields = Model.fields(kind, item);
    if (!fields.length) {
        panel.append(make('p', 'hint', t('palette.noArgs')));
    }
    fields.forEach(name => panel.append(input(name, item.values[name], v => update(kind, index, name, v), entry && entry.args[name])));
    const actions = make('div', 'panel-actions');
    if (kind === 'filter') {
        actions.append(button(t('panel.before'), () => move(kind, index, index - 1)), button(t('panel.after'), () => move(kind, index, index + 1)));
    }
    actions.append(button(t('panel.delete'), () => remove(kind, index), 'danger'));
    panel.append(actions);
}

function renderPanel() {
    const panel = el('panel');
    panel.replaceChildren();
    if (!route()) {
        panel.append(make('p', 'hint', t('panel.none')));
    } else if (selection.kind === 'target') {
        renderTargetPanel(panel);
    } else {
        renderStepPanel(panel);
    }
}

// Chemin d'exemple déduit du premier motif Path : /api/orders/** donne /api/orders/42
function defaultPath(r) {
    const path = r.predicates.find(p => p.name === 'Path');
    const pattern = path ? (path.values.patterns || '').split(',')[0].trim() : '/';
    return pattern.replace(/\/\*\*$/, '/42').replace(/\{\*?(\w+)(:[^}]*)?\}/g, '42').replace(/\*/g, 'x') || '/';
}

function renderTest() {
    const box = el('live-test');
    box.replaceChildren();
    if (!route()) {
        return;
    }
    if (test.path === null) {
        test.path = defaultPath(route());
    }
    const line = make('div', 'test-line');
    const method = make('select');
    method.setAttribute('aria-label', t('test.method'));
    METHODS.forEach(name => method.append(new Option(name, name, false, name === test.method)));
    method.addEventListener('change', () => { test.method = method.value; schedule(); });
    const path = make('input', 'mono grow');
    path.value = test.path;
    path.setAttribute('aria-label', t('test.path'));
    path.addEventListener('input', () => { test.path = path.value; schedule(); });
    line.append(make('span', 'test-label', t('test.title')), method, path);
    box.append(line, make('div', 'test-result'), make('ul', 'lint'));
}

function stepLine(step) {
    if (step.pathBefore != null && step.pathAfter != null && step.pathBefore !== step.pathAfter) {
        return `${step.filter} : ${step.pathBefore} → ${step.pathAfter}`;
    }
    return step.filter + (step.note ? ' : ' + step.note : '');
}

function renderSimulation(view) {
    const result = document.querySelector('#live-test .test-result');
    if (!result) {
        return;
    }
    result.replaceChildren();
    if (view.error) {
        result.append(make('p', 'test-error', '✘ ' + view.error));
    } else if (!view.matched) {
        result.append(make('p', 'test-ko', '✘ ' + t('test.noMatch', view.predicate || '')));
        if (view.predicateError) {
            result.append(make('p', 'test-step mono', view.predicateError));
        }
    } else {
        result.append(make('p', 'test-ok', '✔ ' + view.targetUrl));
        view.steps.forEach(step => result.append(make('p', 'test-step mono sim-' + step.status.toLowerCase(), stepLine(step))));
        const vars = Object.entries(view.variables || {});
        if (vars.length) {
            result.append(make('p', 'test-step mono', t('test.variables', vars.map(([k, v]) => k + '=' + v).join(', '))));
        }
    }
}

function renderAdvice(advice) {
    const list = document.querySelector('#live-test .lint');
    if (list) {
        list.replaceChildren(...advice.map(a => make('li', String(a.level).toUpperCase() === 'ERROR' ? 'lint-error' : 'lint-warn', a.message)));
    }
}

async function renderExport() {
    document.querySelectorAll('[data-export]').forEach(link => {
        link.classList.toggle('active', link.dataset.export === exportMode);
        link.setAttribute('aria-current', String(link.dataset.export === exportMode));
    });
    const content = Workspace.exported(exportMode);
    const yaml = el('yaml');
    if (!content) {
        yaml.textContent = '# ' + t('export.none');
        return;
    }
    try {
        yaml.textContent = await Api.yaml(content.routes, content.comments);
    } catch (error) {
        yaml.textContent = '# ' + t('error.api');
    }
}

// Appels au serveur différés pendant la saisie ; une réponse arrivée après une saisie plus récente est ignorée
function schedule() {
    clearTimeout(timer);
    timer = setTimeout(evaluate, DELAY);
}

async function evaluate() {
    const current = ++sequence;
    renderExport();
    const r = route();
    if (!r) {
        return;
    }
    const request = { method: test.method, path: test.path || '/' };
    const [simulation, advice] = await Promise.all([
        Api.simulate(r, request).catch(() => ({ error: t('error.api') })),
        Api.advice(r, Workspace.others()).catch(() => [])
    ]);
    if (current === sequence) {
        renderSimulation(simulation);
        renderAdvice(advice);
    }
}

export function render() {
    renderSidebar();
    renderContext();
    renderCanvas();
    renderPanel();
    renderTest();
    schedule();
}

// Après une saisie : tout sauf le formulaire, pour ne pas perdre le focus
function refresh() {
    Workspace.save();
    renderSidebar();
    renderContext();
    Canvas.refreshLabels(route());
    schedule();
}

function select(kind, index) {
    selection = { kind, index };
    render();
    // Le panneau est redessiné : on y place le focus pour enchaîner la saisie au clavier
    el('panel').querySelector('input')?.focus();
}

function add(kind, name) {
    const values = {};
    Model.factory(kind, name).fields.forEach(field => { values[field] = ''; });
    items(kind).push({ name, values });
    Workspace.save();
    select(kind, items(kind).length - 1);
}

function remove(kind, index) {
    items(kind).splice(index, 1);
    Workspace.save();
    select('target', 0);
}

function move(kind, from, to) {
    const list = items(kind);
    if (to < 0 || to >= list.length || from === to) {
        return;
    }
    const [item] = list.splice(from, 1);
    list.splice(to, 0, item);
    Workspace.save();
    select(kind, to);
}

function update(kind, index, field, value) {
    if (kind === 'target') {
        route()[field] = value;
    } else {
        items(kind)[index].values[field] = value;
    }
    refresh();
}

export function setExportMode(mode) {
    exportMode = mode;
    renderExport();
}

export function redrawCanvas() {
    renderCanvas();
}
