/*
 * Éditeur graphique (ADR 0011) : appels à l'API de l'éditeur, sous {base-path}/api/editor.
 * Les appels POST renvoient le jeton CSRF de Spring Security quand la page en fournit un.
 */
const root = document.getElementById('editor');
const base = root.dataset.api;

const ACCEPT = { json: 'application/json', yaml: 'application/yaml', html: 'text/html' };

function headers(accept) {
    const result = { Accept: ACCEPT[accept] };
    if (root.dataset.csrfHeader && root.dataset.csrfToken) {
        result[root.dataset.csrfHeader] = root.dataset.csrfToken;
    }
    return result;
}

async function get(path) {
    const response = await fetch(base + path, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
    if (!response.ok) {
        throw new Error(path + ' : HTTP ' + response.status);
    }
    return response.json();
}

async function post(path, body, accept = 'json') {
    const response = await fetch(base + path, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { ...headers(accept), 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    if (!response.ok) {
        throw new Error(path + ' : HTTP ' + response.status);
    }
    return accept === 'json' ? response.json() : response.text();
}

export const Api = {
    routes: () => get('/routes'),
    javaRoutes: () => get('/java-routes'),
    factories: () => get('/factories'),
    catalog: () => get('/catalog'),
    yaml: (routes, comments) => post('/yaml', { routes, comments }, 'yaml'),
    test: (route, routes, request) => post('/test', { route, routes, ...request }, 'html'),
    advice: (route, routes) => post('/advice', { route, routes })
};
