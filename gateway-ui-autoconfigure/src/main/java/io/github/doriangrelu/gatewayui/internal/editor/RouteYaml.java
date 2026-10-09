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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Génère le YAML de configuration de routes (ADR 0012), à reporter dans {@code spring.cloud.gateway.server.webflux.routes}.
 *
 * <p>Les prédicats et filtres sont écrits en forme raccourcie ({@code StripPrefix=1}) quand leurs arguments
 * correspondent aux champs de la fabrique, sinon en forme développée ({@code name} et {@code args}).
 */
public class RouteYaml {

    private static final String ROUTE_INDENT = "            ";

    private static final String PROPERTY_INDENT = ROUTE_INDENT + "  ";

    private static final String ITEM_INDENT = PROPERTY_INDENT + "  ";

    /** Valeurs à mettre entre guillemets pour rester une simple chaîne en YAML. */
    private static final Pattern NEEDS_QUOTES = Pattern.compile("^$|: | #|^[-?:,\\[\\]{}#&*!|>'\"%@`\\s]|\\s$");

    private final FactoryCatalog catalog;

    /**
     * Crée le générateur.
     *
     * @param catalog fabriques de la Gateway, pour l'ordre des arguments
     */
    public RouteYaml(final FactoryCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * YAML d'une liste de routes.
     *
     * @param routes routes à écrire, dans l'ordre
     * @param comments lignes de commentaire placées en tête
     * @return le document YAML
     */
    public String toYaml(final List<EditableRoute> routes, final List<String> comments) {
        final List<String> lines = new ArrayList<>();
        comments.forEach(comment -> lines.add("# " + comment));
        lines.addAll(List.of("spring:", "  cloud:", "    gateway:", "      server:", "        webflux:", "          routes:"));
        if (routes.isEmpty()) {
            lines.add(ROUTE_INDENT + "[]");
        }
        routes.forEach(route -> lines.addAll(route(route)));
        return String.join("\n", lines) + "\n";
    }

    private List<String> route(final EditableRoute route) {
        final List<String> lines = new ArrayList<>();
        lines.add(ROUTE_INDENT + "- id: " + value(route.id()));
        lines.add(PROPERTY_INDENT + "uri: " + value(route.uri()));
        if (route.order() != 0) {
            lines.add(PROPERTY_INDENT + "order: " + route.order());
        }
        lines.addAll(steps("predicates", route.predicates(), catalog::predicate));
        lines.addAll(steps("filters", route.filters(), catalog::filter));
        return lines;
    }

    private static List<String> steps(final String key, final List<EditableStep> steps,
            final Function<String, FactoryDescriptor> factories) {
        if (steps.isEmpty()) {
            return List.of();
        }
        final List<String> lines = new ArrayList<>();
        lines.add(PROPERTY_INDENT + key + ":");
        steps.forEach(step -> lines.addAll(step(step, factories.apply(step.name()))));
        return lines;
    }

    private static List<String> step(final EditableStep step, final FactoryDescriptor factory) {
        if (!factory.fields().containsAll(step.values().keySet())) {
            return expanded(step);
        }
        return List.of(ITEM_INDENT + "- " + value(shortcut(step, factory)));
    }

    /**
     * Forme raccourcie : {@code Nom=valeur1, valeur2}, sans les arguments finaux laissés vides.
     *
     * @param step prédicat ou filtre
     * @param factory description de sa fabrique
     * @return la forme raccourcie
     */
    static String shortcut(final EditableStep step, final FactoryDescriptor factory) {
        final List<String> values = new ArrayList<>(factory.fields().stream()
                .map(field -> step.values().getOrDefault(field, "").trim()).toList());
        while (!values.isEmpty() && values.getLast().isEmpty()) {
            values.removeLast();
        }
        return values.isEmpty() ? step.name() : step.name() + "=" + String.join(", ", values);
    }

    /** Forme développée, pour les arguments que la forme raccourcie ne sait pas exprimer. */
    private static List<String> expanded(final EditableStep step) {
        final List<String> lines = new ArrayList<>();
        lines.add(ITEM_INDENT + "- name: " + value(step.name()));
        lines.add(ITEM_INDENT + "  args:");
        step.values().forEach((key, val) -> lines.add(ITEM_INDENT + "    " + value(key) + ": " + value(val)));
        return lines;
    }

    /**
     * Valeur YAML, mise entre guillemets simples quand YAML l'interpréterait autrement.
     *
     * <p>Les valeurs sont conservées telles qu'elles s'écrivent dans la configuration : un placeholder reste
     * {@code ${NOM}}, et un dollar suivi d'une accolade, littéral (remplacement de {@code RewritePath}), est déjà
     * précédé d'un antislash.
     *
     * @param raw valeur telle qu'elle s'écrit dans la configuration
     * @return la valeur à écrire
     */
    static String value(final String raw) {
        final String text = raw == null ? "" : raw;
        return NEEDS_QUOTES.matcher(text).find() ? "'" + text.replace("'", "''") + "'" : text;
    }
}
