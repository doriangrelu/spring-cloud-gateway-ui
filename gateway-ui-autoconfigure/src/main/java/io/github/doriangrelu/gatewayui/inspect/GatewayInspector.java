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
package io.github.doriangrelu.gatewayui.inspect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Lecture seule de la configuration effective de la Gateway : routes, définitions, filtres globaux et
 * {@code default-filters}.
 *
 * <p>Les routes sont lues à chaque appel depuis le {@link RouteLocator} de la Gateway, qui les met en cache : l'UI
 * reflète donc les rafraîchissements de routes sans redémarrage.
 */
public class GatewayInspector {

    private final RouteLocator routeLocator;

    private final RouteDefinitionLocator routeDefinitionLocator;

    private final List<FilterStep> globalFilters;

    private final List<FilterDefinition> defaultFilters;

    /**
     * Crée un inspecteur.
     *
     * @param routeLocator routes effectives
     * @param routeDefinitionLocator définitions des routes déclaratives
     * @param globalFilters filtres globaux, dans l'ordre où la Gateway les reçoit
     * @param globalFilterBeans filtres globaux indexés par nom de bean, pour nommer les lambdas
     * @param gatewayProperties configuration de la Gateway, ou {@code null} si elle n'est pas disponible
     */
    public GatewayInspector(final RouteLocator routeLocator, final RouteDefinitionLocator routeDefinitionLocator,
            final List<GlobalFilter> globalFilters, final Map<String, GlobalFilter> globalFilterBeans,
            final GatewayProperties gatewayProperties) {
        this.routeLocator = routeLocator;
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.globalFilters = describeGlobalFilters(globalFilters, globalFilterBeans);
        this.defaultFilters = gatewayProperties != null ? List.copyOf(gatewayProperties.getDefaultFilters()) : List.of();
    }

    /**
     * Routes effectives, dans l'ordre où la Gateway les évalue.
     *
     * @return les routes de la Gateway
     */
    public Flux<Route> rawRoutes() {
        return routeLocator.getRoutes();
    }

    /**
     * Définitions des routes déclaratives, indexées par id. Les routes Java DSL n'en ont pas.
     *
     * @return les définitions de routes
     */
    public Mono<Map<String, RouteDefinition>> definitions() {
        return routeDefinitionLocator.getRouteDefinitions()
                .filter(definition -> definition.getId() != null)
                .collectMap(RouteDefinition::getId);
    }

    /**
     * Vues de toutes les routes, dans l'ordre d'évaluation.
     *
     * @return les routes
     */
    public Mono<List<RouteView>> routes() {
        return Mono.zip(rawRoutes().collectList(), definitions())
                .map(tuple -> toViews(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Vues des routes correspondant à une recherche.
     *
     * @param query texte recherché ; vide ou {@code null} pour toutes les routes
     * @return les routes correspondantes
     */
    public Mono<List<RouteView>> routes(final String query) {
        if (query == null || query.isBlank()) {
            return routes();
        }
        return routes().map(routes -> routes.stream().filter(route -> route.matches(query.trim())).toList());
    }

    /**
     * Vue d'une route.
     *
     * @param id identifiant de la route
     * @return la route, ou vide si elle n'existe pas
     */
    public Mono<RouteView> route(final String id) {
        return routes().flatMap(routes -> Mono.justOrEmpty(routes.stream().filter(route -> route.id().equals(id)).findFirst()));
    }

    /**
     * Filtres globaux triés par ordre d'exécution.
     *
     * @return les filtres globaux
     */
    public List<FilterStep> globalFilters() {
        return globalFilters.stream().sorted(Comparator.comparingInt(FilterStep::order)).toList();
    }

    /**
     * Filtres appliqués par défaut à toutes les routes déclaratives ({@code default-filters}).
     *
     * @return les filtres par défaut
     */
    public List<FilterDefinition> defaultFilters() {
        return defaultFilters;
    }

    /**
     * Chaîne complète exécutée pour une route : filtres globaux et filtres de route triés par ordre, exactement comme
     * {@code FilteringWebHandler} (tri stable, filtres globaux en premier à ordre égal).
     *
     * @param route route concernée
     * @return la chaîne de filtres, dans l'ordre d'exécution
     */
    public List<FilterStep> pipeline(final RouteView route) {
        final List<FilterStep> combined = new ArrayList<>(globalFilters);
        combined.addAll(route.filters());
        combined.sort(Comparator.comparingInt(FilterStep::order));
        return combined;
    }

    private static List<FilterStep> describeGlobalFilters(final List<GlobalFilter> filters,
            final Map<String, GlobalFilter> beans) {
        final Map<GlobalFilter, String> beanNames = new IdentityHashMap<>();
        beans.forEach((name, filter) -> beanNames.put(filter, name));
        // Même ordre de départ que FilteringWebHandler : le tri stable donnera la même chaîne
        return filters.stream().map(filter -> FilterStep.ofGlobal(filter, beanNames.get(filter))).toList();
    }

    private static List<RouteView> toViews(final List<Route> routes, final Map<String, RouteDefinition> definitions) {
        return routes.stream().map(route -> toView(route, definitions.get(route.getId()))).toList();
    }

    private static RouteView toView(final Route route, final RouteDefinition definition) {
        final List<Definition> predicates = definition == null ? List.of()
                : definition.getPredicates().stream().map(Definition::of).toList();
        final List<Definition> filters = definition == null ? List.of()
                : definition.getFilters().stream().map(Definition::of).toList();
        return new RouteView(route.getId(), route.getUri(), route.getOrder(), String.valueOf(route.getPredicate()),
                predicates, filters, route.getFilters().stream().map(FilterStep::ofRoute).toList(),
                new LinkedHashMap<>(route.getMetadata()), definition != null);
    }
}
