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
package io.github.doriangrelu.gatewayui.internal.editor;

import java.util.List;

/**
 * Fabrique de prédicats ou de filtres, telle que présentée dans le catalogue (ADR 0013). Les textes (résumé, détails,
 * arguments, exemple) sont dans les fichiers de messages, sous {@code catalog.<kind>.<name>.*}.
 *
 * @param kind {@code predicate} ou {@code filter}
 * @param name nom de la fabrique, tel qu'écrit dans la configuration
 * @param category code de la catégorie ({@code custom} pour une fabrique maison, {@code other} si inconnue)
 * @param fields champs de la fabrique, dans l'ordre de la forme raccourcie
 * @param documentation lien vers la documentation officielle de Spring Cloud Gateway, {@code null} pour une fabrique maison
 * @param custom fabrique propre à l'application, hors Spring Cloud Gateway
 */
public record CatalogEntry(String kind, String name, String category, List<String> fields, String documentation, boolean custom) {

    /** Type d'une fabrique de prédicats. */
    public static final String PREDICATE = "predicate";

    /** Type d'une fabrique de filtres. */
    public static final String FILTER = "filter";

    /**
     * Copie les champs.
     *
     * @param kind {@code predicate} ou {@code filter}
     * @param name nom de la fabrique
     * @param category code de la catégorie
     * @param fields champs de la fabrique
     * @param documentation lien vers la documentation officielle
     * @param custom fabrique maison
     */
    public CatalogEntry {
        fields = List.copyOf(fields);
    }

    /**
     * Préfixe des clés de message de la fabrique.
     *
     * @return {@code catalog.<kind>.<name>}
     */
    public String messageKey() {
        return "catalog." + kind + "." + name;
    }
}
