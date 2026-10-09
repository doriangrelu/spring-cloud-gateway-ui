/*
 * Éditeur graphique : textes traduits, fournis par la page (ADR 0009). Aucun texte affiché n'est écrit dans les scripts.
 */
const messages = {};
document.querySelectorAll('#editor-messages [data-key]').forEach(element => {
    messages[element.dataset.key] = element.textContent;
});

/**
 * Texte d'un message, avec ses arguments {0}, {1}... Comme MessageFormat côté Java, l'apostrophe d'un message avec
 * arguments est écrite '' dans les fichiers de messages.
 */
export function t(key, ...args) {
    const pattern = messages[key];
    if (pattern === undefined) {
        return '[' + key + ']';
    }
    if (!args.length) {
        return pattern;
    }
    return pattern.replace(/''/g, '\u0000').replace(/\{(\d+)\}/g, (match, index) => String(args[index] ?? match)).replace(/\u0000/g, '\'');
}
