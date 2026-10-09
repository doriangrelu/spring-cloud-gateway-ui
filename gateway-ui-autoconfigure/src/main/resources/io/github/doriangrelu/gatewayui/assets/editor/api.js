/*
 * Éditeur graphique (ADR 0011) : appels à l'API de l'éditeur, sous {base-path}/api/editor.
 * Les appels POST renvoient le jeton CSRF de Spring Security quand la page en fournit un.
 */
const root = document.getElementById('editor');
const base = root.dataset.api;

function headers(json) {
    const result = { Accept: json ? 'application/json' : 'application/yaml' };
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

async function post(path, body, json = true) {
    const response = await fetch(base + path, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { ...headers(json), 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    if (!response.ok) {
        throw new Error(path + ' : HTTP ' + response.status);
    }
    return json ? response.json() : response.text();
}

export const Api = {
    routes: () => get('/routes'),
    javaRoutes: () => get('/java-routes'),
    factories: () => get('/factories'),
    yaml: (routes, comments) => post('/yaml', { routes, comments }, false),
    simulate: (route, request) => post('/simulate', { route, ...request }),
    advice: (route, routes) => post('/advice', { route, routes })
};
