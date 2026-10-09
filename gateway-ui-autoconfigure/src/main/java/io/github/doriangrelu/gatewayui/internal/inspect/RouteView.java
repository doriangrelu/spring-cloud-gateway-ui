/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.doriangrelu.gatewayui.internal.inspect;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vue d'une route de la Gateway.
 *
 * @param id identifiant de la route
 * @param uri URI cible
 * @param order ordre d'évaluation, les plus petits en premier
 * @param predicate prédicat tel que décrit par la Gateway (seule information disponible pour une route Java DSL)
 * @param predicateDefinitions prédicats déclarés, vides pour une route Java DSL
 * @param filterDefinitions filtres déclarés (hors {@code default-filters}), vides pour une route Java DSL
 * @param filters filtres effectifs de la route, {@code default-filters} compris, dans leur ordre d'exécution
 * @param metadata métadonnées de la route
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

    /**
     * Schéma de l'URI cible en minuscules ({@code http}, {@code lb}, {@code forward}...).
     *
     * @return le schéma, vide s'il est absent
     */
    public String scheme() {
        final String scheme = uri.getScheme();
        return scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
    }

    /**
     * Résumé court du prédicat pour les listes : la déclaration si elle est connue, sinon la description de la Gateway.
     *
     * @return le résumé du prédicat
     */
    public String predicateSummary() {
        return predicateDefinitions.isEmpty() ? predicate : Definition.summary(predicateDefinitions);
    }

    /**
     * Indique si la route correspond à une recherche, sur son id, son URI ou ses prédicats, sans tenir compte de la casse.
     *
     * @param query texte recherché
     * @return {@code true} si la route correspond
     */
    public boolean matches(final String query) {
        final String needle = query.toLowerCase(Locale.ROOT);
        return contains(id, needle) || contains(uri.toString(), needle) || contains(predicateSummary(), needle);
    }

    private static boolean contains(final String text, final String needle) {
        return text.toLowerCase(Locale.ROOT).contains(needle);
    }
}
