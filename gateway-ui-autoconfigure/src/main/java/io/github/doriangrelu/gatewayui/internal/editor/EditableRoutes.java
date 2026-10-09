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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.support.NameUtils;

/**
 * Convertit les définitions de la Gateway en routes éditables.
 *
 * <p>Les arguments d'une forme raccourcie ({@code RewritePath=/a, /b}) arrivent indexés ({@code _genkey_0},
 * {@code _genkey_1}...) : ils sont rattachés aux champs de la fabrique, comme le fait la Gateway en les liant.
 */
public class EditableRoutes {

    private static final Pattern BOOLEAN = Pattern.compile("true|false");

    private final FactoryCatalog catalog;

    /**
     * Crée le convertisseur.
     *
     * @param catalog fabriques de la Gateway
     */
    public EditableRoutes(final FactoryCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Route éditable correspondant à une définition.
     *
     * @param definition définition de la Gateway
     * @return la route éditable
     */
    public EditableRoute of(final RouteDefinition definition) {
        return new EditableRoute(definition.getId(), String.valueOf(definition.getUri()), definition.getOrder(),
                steps(definition.getPredicates().stream().map(p -> Map.entry(p.getName(), p.getArgs())).toList(), catalog::predicate),
                steps(definition.getFilters().stream().map(f -> Map.entry(f.getName(), f.getArgs())).toList(), catalog::filter));
    }

    /**
     * Route éditable correspondant à une définition, avec les placeholders d'origine rétablis (ADR 0012).
     *
     * <p>Une valeur n'est remplacée par sa valeur brute que si celle-ci contient un placeholder : les autres valeurs
     * sont identiques. Une route absente de la configuration (Java, API d'actuator) garde ses valeurs résolues.
     *
     * @param definition définition de la Gateway
     * @param raw valeurs brutes de la configuration
     * @return la route éditable
     */
    public EditableRoute of(final RouteDefinition definition, final RawRouteConfiguration raw) {
        final EditableRoute resolved = of(definition);
        return raw.routePrefix(definition.getId()).map(prefix -> restore(resolved, definition, prefix, raw)).orElse(resolved);
    }

    private EditableRoute restore(final EditableRoute resolved, final RouteDefinition definition, final String prefix,
            final RawRouteConfiguration raw) {
        final String uri = raw.raw(prefix + ".uri").filter(EditableRoutes::hasPlaceholder).orElse(resolved.uri());
        final List<EditableStep> predicates = restore(resolved.predicates(),
                definition.getPredicates().stream().map(p -> p.getArgs()).toList(), prefix + ".predicates", true, raw);
        final List<EditableStep> filters = restore(resolved.filters(),
                definition.getFilters().stream().map(f -> f.getArgs()).toList(), prefix + ".filters", false, raw);
        return new EditableRoute(resolved.id(), uri, resolved.order(), predicates, filters);
    }

    private List<EditableStep> restore(final List<EditableStep> steps, final List<Map<String, String>> resolvedArgs,
            final String prefix, final boolean predicate, final RawRouteConfiguration raw) {
        final List<EditableStep> restored = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            final EditableStep step = steps.get(i);
            final FactoryDescriptor factory = predicate ? catalog.predicate(step.name()) : catalog.filter(step.name());
            final EditableStep original = rawStep(step.name(), prefix + "[" + i + "]", predicate, resolvedArgs.get(i), raw, factory);
            restored.add(withPlaceholders(step, original));
        }
        return restored;
    }

    private static EditableStep rawStep(final String name, final String prefix, final boolean predicate,
            final Map<String, String> resolvedArgs, final RawRouteConfiguration raw, final FactoryDescriptor factory) {
        try {
            return step(name, raw.args(prefix, predicate, resolvedArgs), factory);
        }
        catch (final RuntimeException ex) {
            // Forme raccourcie illisible : on garde les valeurs résolues
            return new EditableStep(name, Map.of());
        }
    }

    private static EditableStep withPlaceholders(final EditableStep resolved, final EditableStep raw) {
        final Map<String, String> values = new LinkedHashMap<>(resolved.values());
        values.replaceAll((field, value) -> {
            final String original = raw.values().get(field);
            return original != null && hasPlaceholder(original) ? original : value;
        });
        return new EditableStep(resolved.name(), values);
    }

    private static boolean hasPlaceholder(final String value) {
        return value.contains("${");
    }

    private static List<EditableStep> steps(final List<Map.Entry<String, Map<String, String>>> declared,
            final Function<String, FactoryDescriptor> factories) {
        return declared.stream().map(d -> step(d.getKey(), d.getValue(), factories.apply(d.getKey()))).toList();
    }

    /**
     * Rattache les arguments d'un prédicat ou d'un filtre aux champs de sa fabrique.
     *
     * @param name nom de la fabrique
     * @param args arguments tels que déclarés
     * @param factory description de la fabrique
     * @return l'étape éditable
     */
    static EditableStep step(final String name, final Map<String, String> args, final FactoryDescriptor factory) {
        final boolean shortcut = args.keySet().stream().allMatch(key -> key.startsWith(NameUtils.GENERATED_NAME_PREFIX));
        if (!shortcut) {
            return new EditableStep(name, args);
        }
        final List<String> values = new ArrayList<>(args.values());
        return new EditableStep(name, factory.gathersList() ? gathered(values, factory) : positional(values, factory));
    }

    private static Map<String, String> gathered(final List<String> values, final FactoryDescriptor factory) {
        final Map<String, String> result = new LinkedHashMap<>();
        String flag = null;
        if (factory.hasTailFlag() && !values.isEmpty() && BOOLEAN.matcher(values.getLast()).matches()) {
            flag = values.removeLast();
        }
        result.put(field(factory, 0), String.join(", ", values));
        if (flag != null) {
            result.put(field(factory, 1), flag);
        }
        return result;
    }

    private static Map<String, String> positional(final List<String> values, final FactoryDescriptor factory) {
        final Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) {
            result.put(field(factory, i), values.get(i));
        }
        return result;
    }

    private static String field(final FactoryDescriptor factory, final int index) {
        return index < factory.fields().size() ? factory.fields().get(index) : "arg" + index;
    }
}
