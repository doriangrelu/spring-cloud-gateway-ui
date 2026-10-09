package dev.gatewayui.inspect;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vue d'une route de la Gateway.
 *
 * @param predicate prédicat tel que décrit par la Gateway (seule information disponible pour une route Java DSL)
 * @param predicateDefinitions prédicats déclarés, vides pour une route Java DSL
 * @param filterDefinitions filtres déclarés (hors default-filters), vides pour une route Java DSL
 * @param filters filtres effectifs de la route, default-filters compris, dans leur ordre d'exécution
 * @param declarative {@code true} si la route provient d'une {@code RouteDefinition} (YAML, properties, API...)
 */
public record RouteView(
        String id,
        URI uri,
        int order,
        String predicate,
        List<Definition> predicateDefinitions,
        List<Definition> filterDefinitions,
        List<FilterStep> filters,
        Map<String, Object> metadata,
        boolean declarative) {

    public String scheme() {
        String scheme = uri.getScheme();
        return scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
    }

    /** Résumé court du prédicat pour les listes. */
    public String predicateSummary() {
        return predicateDefinitions.isEmpty() ? predicate : Definition.summary(predicateDefinitions);
    }

    /** Texte sur lequel porte la recherche de la liste des routes. */
    boolean matches(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return id.toLowerCase(Locale.ROOT).contains(needle)
                || uri.toString().toLowerCase(Locale.ROOT).contains(needle)
                || predicateSummary().toLowerCase(Locale.ROOT).contains(needle);
    }
}
