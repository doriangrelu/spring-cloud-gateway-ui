/*
 * SPIKE #28 : chargement des données de la Gateway et branchement de la page.
 */
(async () => {
    const api = path => fetch('/gateway-ui/editor-proto/api/' + path).then(response => response.json());
    const [factories, definitions, javaRoutes] = await Promise.all([api('factories'), api('routes'), api('java-routes')]);
    Model.setFactories(factories);
    Workspace.init(definitions, javaRoutes);
    document.getElementById('restored').hidden = !Workspace.wasRestored();

    document.querySelectorAll('[data-export]').forEach(link => link.addEventListener('click', event => {
        event.preventDefault();
        Editor.setExportMode(link.dataset.export);
    }));
    document.getElementById('copy-yaml').addEventListener('click', async event => {
        await navigator.clipboard.writeText(document.getElementById('yaml').textContent);
        event.target.textContent = 'Copié ✔';
        setTimeout(() => { event.target.textContent = 'Copier'; }, 1500);
    });
    document.getElementById('reset-workspace').addEventListener('click', () => {
        if (confirm('Abandonner toutes les modifications et repartir de la configuration de la Gateway ?')) {
            Workspace.resetAll();
            location.reload();
        }
    });
    // « / » ouvre la palette des filtres, comme dans de nombreux outils
    document.addEventListener('keydown', event => {
        const typing = ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName);
        if (event.key === '/' && !typing && !document.getElementById('palette').open) {
            event.preventDefault();
            document.querySelector('.context-actions .primary')?.click();
        }
    });
    window.addEventListener('resize', () => Editor.render());

    Editor.render();
})();
