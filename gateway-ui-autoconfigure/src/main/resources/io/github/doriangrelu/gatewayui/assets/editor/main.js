/*
 * Éditeur graphique : chargement des données de la Gateway et branchement de la page.
 */
import { Api } from './api.js';
import { Catalog } from './catalog.js';
import { redrawCanvas, render, setExportMode } from './editor.js';
import { t } from './i18n.js';
import { Model } from './model.js';
import { Palette } from './palette.js';
import { Workspace } from './workspace.js';

function bindExport() {
    document.querySelectorAll('[data-export]').forEach(link => link.addEventListener('click', event => {
        event.preventDefault();
        setExportMode(link.dataset.export);
    }));
    const copy = document.getElementById('copy-yaml');
    const label = copy.textContent;
    copy.addEventListener('click', async () => {
        await navigator.clipboard.writeText(document.getElementById('yaml').textContent);
        copy.textContent = t('export.copied');
        setTimeout(() => { copy.textContent = label; }, 1500);
    });
}

function bindReset() {
    const reset = document.getElementById('reset-workspace');
    reset.hidden = false;
    reset.addEventListener('click', () => {
        if (confirm(t('reset.confirm'))) {
            Workspace.clear();
            location.reload();
        }
    });
}

// « / » ouvre la palette des filtres, comme dans de nombreux outils
function bindShortcuts() {
    document.addEventListener('keydown', event => {
        const typing = ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName);
        if (event.key === '/' && !typing && !Palette.isOpen()) {
            event.preventDefault();
            document.querySelector('.context-actions .add-filter')?.click();
        }
    });
    let resize = null;
    window.addEventListener('resize', () => {
        clearTimeout(resize);
        resize = setTimeout(redrawCanvas, 100);
    });
}

async function start() {
    try {
        const [factories, catalog, routes, javaRoutes] = await Promise.all([Api.factories(), Api.catalog(), Api.routes(), Api.javaRoutes()]);
        Model.setFactories(factories);
        Catalog.set(catalog);
        Workspace.init(routes, javaRoutes);
    } catch (error) {
        document.getElementById('load-error').hidden = false;
        return;
    }
    document.getElementById('restored').hidden = !Workspace.wasRestored();
    bindExport();
    bindReset();
    bindShortcuts();
    render();
}

start();
