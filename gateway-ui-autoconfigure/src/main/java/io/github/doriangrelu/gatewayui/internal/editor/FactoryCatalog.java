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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;

/**
 * Fabriques de prédicats et de filtres disponibles dans la Gateway, y compris les fabriques maison de l'application.
 */
public class FactoryCatalog {

    private final Map<String, FactoryDescriptor> predicates;

    private final Map<String, FactoryDescriptor> filters;

    /**
     * Construit le catalogue à partir des fabriques déclarées dans la Gateway.
     *
     * @param predicates fabriques de prédicats
     * @param filters fabriques de filtres
     */
    public FactoryCatalog(final List<RoutePredicateFactory<?>> predicates, final List<GatewayFilterFactory<?>> filters) {
        this.predicates = index(predicates.stream().map(f -> FactoryDescriptor.of(f.name(), f)).toList());
        this.filters = index(filters.stream().map(f -> FactoryDescriptor.of(f.name(), f)).toList());
    }

    /**
     * Fabriques de prédicats, triées par nom.
     *
     * @return les descriptions
     */
    public List<FactoryDescriptor> predicates() {
        return sorted(predicates);
    }

    /**
     * Fabriques de filtres, triées par nom.
     *
     * @return les descriptions
     */
    public List<FactoryDescriptor> filters() {
        return sorted(filters);
    }

    /**
     * Description d'une fabrique de prédicat.
     *
     * @param name nom de la fabrique
     * @return la description, ou une description sans champ si la fabrique est inconnue
     */
    public FactoryDescriptor predicate(final String name) {
        return predicates.getOrDefault(name, FactoryDescriptor.unknown(name));
    }

    /**
     * Description d'une fabrique de filtre.
     *
     * @param name nom de la fabrique
     * @return la description, ou une description sans champ si la fabrique est inconnue
     */
    public FactoryDescriptor filter(final String name) {
        return filters.getOrDefault(name, FactoryDescriptor.unknown(name));
    }

    private static Map<String, FactoryDescriptor> index(final List<FactoryDescriptor> descriptors) {
        // Une fabrique maison peut reprendre le nom d'une fabrique standard : la dernière déclarée l'emporte, comme dans la Gateway
        return descriptors.stream().collect(Collectors.toMap(FactoryDescriptor::name, Function.identity(), (first, last) -> last));
    }

    private static List<FactoryDescriptor> sorted(final Map<String, FactoryDescriptor> descriptors) {
        return descriptors.values().stream().sorted(Comparator.comparing(FactoryDescriptor::name)).toList();
    }
}
