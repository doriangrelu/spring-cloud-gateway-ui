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

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.tester.RouteTester;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator;
import org.springframework.cloud.gateway.support.ConfigurationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Simule exactement une route en cours d'édition (ADR 0011) : elle est construite par le constructeur de routes de la
 * Gateway, avec ses vraies fabriques et ses {@code default-filters}, puis passée au testeur. Elle n'est jamais ajoutée
 * à la Gateway, et ses filtres ne sont jamais exécutés (ADR 0004).
 */
public class EditedRouteSimulator {

    private static final String DEFAULT_ID = "edited-route";

    private final FactoryCatalog catalog;

    private final RouteTester tester;

    private final RouteDefinitionRouteLocatorFactory locators;

    private final UnaryOperator<String> resolver;

    /**
     * Crée le simulateur.
     *
     * @param catalog fabriques de la Gateway, pour reconstituer la forme raccourcie
     * @param tester testeur de routes
     * @param locators construction des routes par la Gateway
     * @param resolver résolution des placeholders, comme lors de la lecture de la configuration
     */
    public EditedRouteSimulator(final FactoryCatalog catalog, final RouteTester tester,
            final RouteDefinitionRouteLocatorFactory locators, final UnaryOperator<String> resolver) {
        this.catalog = catalog;
        this.tester = tester;
        this.locators = locators;
        this.resolver = resolver;
    }

    /**
     * Teste une requête contre la route éditée seule.
     *
     * @param route route éditée
     * @param request requête à tester, préalablement validée
     * @return le résultat, ou l'erreur qui empêche la Gateway de construire la route
     */
    public Mono<Outcome> simulate(final EditableRoute route, final TestRequest request) {
        return simulate(route, List.of(), List.of(), request);
    }

    /**
     * Teste une requête contre la route éditée, évaluée avec d'autres routes : on voit quelle route prend la requête,
     * et laquelle est masquée.
     *
     * <p>Comme le {@code CachingRouteLocator} de la Gateway, les routes sont triées par ordre, sans changer l'ordre de
     * routes de même ordre : celles de la configuration, puis celles déclarées en Java.
     *
     * @param route route éditée
     * @param others autres routes de l'espace de travail ; une route de même identifiant que la route éditée est ignorée
     * @param javaRoutes routes déclarées en Java dans la Gateway, inchangées
     * @param request requête à tester, préalablement validée
     * @return le résultat, ou l'erreur qui empêche la Gateway de construire une des routes
     */
    public Mono<Outcome> simulate(final EditableRoute route, final List<EditableRoute> others, final List<Route> javaRoutes,
            final TestRequest request) {
        return Mono.fromCallable(() -> definitions(route, others))
                .flatMap(definitions -> locators.create(definitions.values()).getRoutes().collectList()
                        .map(built -> ordered(built, javaRoutes))
                        .flatMap(routes -> tester.test(request, routes, definitions)))
                .map(Outcome::success)
                .onErrorResume(error -> Mono.just(Outcome.failure(Message.of("editor.simulation.invalid", describe(error)))));
    }

    private Map<String, RouteDefinition> definitions(final EditableRoute route, final List<EditableRoute> others) {
        final Map<String, RouteDefinition> definitions = new LinkedHashMap<>();
        final RouteDefinition edited = definition(route);
        definitions.put(edited.getId(), edited);
        others.stream().map(this::definition).forEach(definition -> definitions.putIfAbsent(definition.getId(), definition));
        return definitions;
    }

    private static List<Route> ordered(final List<Route> built, final List<Route> javaRoutes) {
        return Stream.concat(built.stream(), javaRoutes.stream()).sorted(Comparator.comparingInt(Route::getOrder)).toList();
    }

    /**
     * Définition de la Gateway équivalente à la route éditée, telle qu'elle serait lue depuis le YAML exporté.
     *
     * @param route route éditée
     * @return la définition, placeholders résolus
     */
    RouteDefinition definition(final EditableRoute route) {
        final RouteDefinition definition = new RouteDefinition();
        definition.setId(route.id().isBlank() ? DEFAULT_ID : route.id());
        definition.setUri(URI.create(resolver.apply(route.uri()).trim()));
        definition.setOrder(route.order());
        definition.setPredicates(route.predicates().stream().map(step -> predicate(step, catalog.predicate(step.name()))).toList());
        definition.setFilters(route.filters().stream().map(step -> filter(step, catalog.filter(step.name()))).toList());
        return definition;
    }

    private PredicateDefinition predicate(final EditableStep step, final FactoryDescriptor factory) {
        final Optional<String> shortcut = shortcut(step, factory);
        if (shortcut.isPresent()) {
            return new PredicateDefinition(shortcut.get());
        }
        final PredicateDefinition definition = new PredicateDefinition();
        definition.setName(step.name());
        definition.setArgs(resolvedArgs(step));
        return definition;
    }

    private FilterDefinition filter(final EditableStep step, final FactoryDescriptor factory) {
        final Optional<String> shortcut = shortcut(step, factory);
        if (shortcut.isPresent()) {
            return new FilterDefinition(shortcut.get());
        }
        final FilterDefinition definition = new FilterDefinition();
        definition.setName(step.name());
        definition.setArgs(resolvedArgs(step));
        return definition;
    }

    /**
     * Forme raccourcie, placeholders résolus, quand les arguments s'y prêtent : elle sera analysée par la Gateway
     * elle-même, exactement comme le YAML exporté.
     */
    private Optional<String> shortcut(final EditableStep step, final FactoryDescriptor factory) {
        final String shortcut = RouteYaml.shortcut(step, factory);
        final boolean expressible = factory.fields().containsAll(step.values().keySet()) && shortcut.contains("=");
        return expressible ? Optional.of(resolver.apply(shortcut)) : Optional.empty();
    }

    private Map<String, String> resolvedArgs(final EditableStep step) {
        final Map<String, String> args = new LinkedHashMap<>();
        step.values().forEach((key, value) -> args.put(key, resolver.apply(value)));
        return args;
    }

    private static String describe(final Throwable error) {
        final Throwable cause = error.getCause() != null && error.getMessage() == null ? error.getCause() : error;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    /**
     * Résultat d'une simulation : le résultat du testeur, ou l'erreur de construction de la route.
     *
     * @param result résultat du testeur, {@code null} en cas d'erreur
     * @param error erreur de construction, {@code null} en cas de succès
     */
    public record Outcome(TestResult result, Message error) {

        static Outcome success(final TestResult result) {
            return new Outcome(result, null);
        }

        static Outcome failure(final Message error) {
            return new Outcome(null, error);
        }
    }

    /**
     * Construit le constructeur de routes de la Gateway pour une définition donnée.
     */
    @FunctionalInterface
    public interface RouteDefinitionRouteLocatorFactory {

        /**
         * Constructeur de routes limité à des définitions.
         *
         * @param definitions définitions des routes éditées
         * @return le constructeur de routes
         */
        RouteDefinitionRouteLocator create(Collection<RouteDefinition> definitions);

        /**
         * Constructeur de routes de la Gateway : ses fabriques, ses {@code default-filters}, et une erreur levée (au lieu
         * d'être ignorée) quand la définition est invalide.
         *
         * @param predicates fabriques de prédicats
         * @param filters fabriques de filtres
         * @param gatewayProperties configuration de la Gateway, pour les {@code default-filters} ; peut être {@code null}
         * @param configurationService liaison des arguments, celle de la Gateway
         * @return la fabrique de constructeurs de routes
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        static RouteDefinitionRouteLocatorFactory of(final List<RoutePredicateFactory<?>> predicates,
                final List<GatewayFilterFactory<?>> filters, final GatewayProperties gatewayProperties,
                final ConfigurationService configurationService) {
            final GatewayProperties properties = new GatewayProperties();
            properties.setFailOnRouteDefinitionError(true);
            if (gatewayProperties != null) {
                properties.setDefaultFilters(gatewayProperties.getDefaultFilters());
            }
            return definitions -> new RouteDefinitionRouteLocator(() -> Flux.fromIterable(definitions), (List) predicates, (List) filters,
                    properties, configurationService);
        }
    }
}
