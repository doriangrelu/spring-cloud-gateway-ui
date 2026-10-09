/*
 * Éditeur graphique : catalogue documenté des prédicats et filtres (ADR 0013), textes déjà traduits par le serveur.
 */
const entries = new Map();
const catalogPage = document.getElementById('editor').dataset.catalog;

export const Catalog = {
    set(list) {
        list.forEach(entry => entries.set(entry.kind + ':' + entry.name, entry));
    },

    // Fabrique inconnue de la Gateway (faute de frappe, filtre retiré) : aucune aide
    get(kind, name) {
        return entries.get(kind + ':' + name) || null;
    },

    page(entry) {
        return catalogPage + '#' + entry.anchor;
    }
};
