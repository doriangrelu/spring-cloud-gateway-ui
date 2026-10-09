/*
 * Éditeur graphique : fabriques de la Gateway (champs de chaque prédicat et filtre) et libellés des nœuds.
 * Le YAML, la simulation et les conseils sont calculés côté serveur (ADR 0011) : ce module ne sert qu'à l'affichage.
 */
let factories = { predicates: [], filters: [] };

export const Model = {
    setFactories(value) {
        factories = value;
    },

    names(kind) {
        return (kind === 'predicate' ? factories.predicates : factories.filters).map(factory => factory.name);
    },

    factory(kind, name) {
        return (kind === 'predicate' ? factories.predicates : factories.filters).find(factory => factory.name === name)
            || { name, fields: [], shortcutType: 'DEFAULT' };
    },

    // Champs de la fabrique, plus ceux d'une configuration développée inconnue de la fabrique
    fields(kind, item) {
        const known = Model.factory(kind, item.name).fields;
        return [...known, ...Object.keys(item.values).filter(field => !known.includes(field))];
    },

    // Arguments sous forme raccourcie, pour le libellé d'un nœud
    summary(kind, item) {
        const values = Model.fields(kind, item).map(field => (item.values[field] || '').trim());
        while (values.length && values[values.length - 1] === '') {
            values.pop();
        }
        return values.join(', ');
    },

    copy: value => JSON.parse(JSON.stringify(value))
};
