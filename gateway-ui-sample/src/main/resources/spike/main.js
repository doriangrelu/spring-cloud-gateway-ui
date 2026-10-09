/*
 * SPIKE #28 : chargement des données de la Gateway et branchement de la barre d'outils.
 */
(async () => {
    const api = path => fetch('/gateway-ui/editor-proto/api/' + path).then(response => response.json());
    const [factories, routes] = await Promise.all([api('factories'), api('routes')]);
    Editor.state.factories = factories;

    const mode = new URLSearchParams(location.search).get('mode') === 'drawflow' ? 'drawflow' : 'canvas';
    const renderer = mode === 'drawflow' ? DrawflowRenderer : CanvasRenderer;
    Editor.useRenderer(renderer);
    document.querySelectorAll('[data-mode]').forEach(link => link.classList.toggle('active', link.dataset.mode === mode));
    document.getElementById('mode-title').textContent = renderer.title;
    document.getElementById('mode-hint').textContent = renderer.hint;

    const options = (select, values) => values.forEach(value => select.add(new Option(value, value)));
    const routeSelect = document.getElementById('route-select');
    options(routeSelect, routes.map(route => route.id));
    options(document.getElementById('predicate-factory'), factories.predicates.map(f => f.name));
    options(document.getElementById('filter-factory'), factories.filters.map(f => f.name));

    const show = id => {
        routeSelect.value = id;
        Editor.load(routes.find(route => route.id === id));
    };
    routeSelect.addEventListener('change', () => show(routeSelect.value));
    document.getElementById('new-route').addEventListener('click', () => Editor.blank());
    document.getElementById('add-predicate').addEventListener('click',
        () => Editor.add('predicate', document.getElementById('predicate-factory').value));
    document.getElementById('add-filter').addEventListener('click',
        () => Editor.add('filter', document.getElementById('filter-factory').value));
    document.getElementById('copy-yaml').addEventListener('click', async event => {
        await navigator.clipboard.writeText(document.getElementById('yaml').textContent);
        event.target.textContent = 'Copié ✔';
        setTimeout(() => { event.target.textContent = 'Copier'; }, 1500);
    });
    window.addEventListener('resize', () => Editor.render());

    show(routes.some(route => route.id === 'users') ? 'users' : routes[0].id);
})();
