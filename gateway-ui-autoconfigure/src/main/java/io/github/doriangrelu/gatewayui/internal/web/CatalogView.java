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
package io.github.doriangrelu.gatewayui.internal.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.editor.Catalog;
import io.github.doriangrelu.gatewayui.internal.editor.CatalogEntry;

/**
 * Fabrique du catalogue avec ses textes traduits dans la langue de l'utilisateur (ADR 0013). Une fabrique non encore
 * documentée l'indique explicitement ; une fabrique maison est signalée comme telle.
 *
 * @param kind {@code predicate} ou {@code filter}
 * @param name nom de la fabrique
 * @param category libellé de la catégorie
 * @param documented la fabrique est documentée dans le catalogue
 * @param summary résumé, ou mention de l'absence de documentation
 * @param details détails, vide si absents
 * @param example exemple de configuration YAML, vide si absent
 * @param args description de chaque champ, dans l'ordre de la forme raccourcie (vide si non documenté)
 * @param documentation lien vers la documentation officielle, {@code null} pour une fabrique maison
 * @param custom fabrique propre à l'application
 * @param anchor ancre de la fabrique sur la page du catalogue
 */
public record CatalogView(String kind, String name, String category, boolean documented, String summary, String details,
        String example, Map<String, String> args, String documentation, boolean custom, String anchor) {

    /**
     * Fabriques d'un type et d'une catégorie.
     *
     * @param kind {@code predicate} ou {@code filter}
     * @param category libellé de la catégorie
     * @param entries fabriques de la catégorie, triées par nom
     */
    public record Group(String kind, String category, List<CatalogView> entries) {
    }

    /**
     * Fabrique, textes traduits.
     *
     * @param entry fabrique du catalogue
     * @param ui contexte de la page, pour la langue
     * @return la vue
     */
    static CatalogView of(final CatalogEntry entry, final UiContext ui) {
        final String key = entry.messageKey();
        final boolean documented = ui.hasMessage(key + ".summary");
        final String summary = documented ? ui.message(key + ".summary")
                : ui.message(entry.custom() ? "catalog.custom" : "catalog.undocumented");
        return new CatalogView(entry.kind(), entry.name(), ui.message("catalog.category." + entry.category()), documented, summary,
                optional(ui, key + ".details"), optional(ui, key + ".example"), args(entry, ui), entry.documentation(), entry.custom(),
                entry.kind() + "-" + entry.name());
    }

    /**
     * Fabriques groupées par type puis par catégorie, dans l'ordre d'affichage du catalogue.
     *
     * @param entries fabriques du catalogue
     * @param ui contexte de la page, pour la langue
     * @return les groupes non vides
     */
    static List<Group> groups(final List<CatalogEntry> entries, final UiContext ui) {
        return List.of(CatalogEntry.PREDICATE, CatalogEntry.FILTER).stream()
                .flatMap(kind -> Catalog.categories(kind).stream().map(category -> group(entries, kind, category, ui)))
                .filter(group -> !group.entries().isEmpty())
                .toList();
    }

    private static Group group(final List<CatalogEntry> entries, final String kind, final String category, final UiContext ui) {
        return new Group(kind, ui.message("catalog.category." + category), entries.stream()
                .filter(entry -> entry.kind().equals(kind) && entry.category().equals(category))
                .map(entry -> of(entry, ui))
                .toList());
    }

    private static Map<String, String> args(final CatalogEntry entry, final UiContext ui) {
        final Map<String, String> args = new LinkedHashMap<>();
        entry.fields().forEach(field -> args.put(field, optional(ui, entry.messageKey() + ".arg." + field)));
        return args;
    }

    private static String optional(final UiContext ui, final String key) {
        return ui.hasMessage(key) ? ui.message(key) : "";
    }
}
