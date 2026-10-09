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
package io.github.doriangrelu.gatewayui.internal.tester;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.inspect.Definition;
import io.github.doriangrelu.gatewayui.internal.inspect.GatewayInspector;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Evaluation;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Outcome;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult.Simulation;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR;

/**
 * Indique quelle route recevrait une requête donnée, et ce que la Gateway transmettrait au service.
 *
 * <p>Toutes les routes sont évaluées, et pas seulement jusqu'à la première qui correspond, pour signaler celles qui
 * sont masquées par une route prioritaire. Aucune requête n'est émise : les prédicats sont évalués sur un échange
 * fictif, puis les filtres de la route retenue sont rejoués par {@link FilterSimulator}.
 */
public class RouteTester {

    private static final Duration PREDICATE_TIMEOUT = Duration.ofSeconds(2);

    private final GatewayInspector inspector;

    /**
     * Crée un testeur.
     *
     * @param inspector inspecteur de la Gateway
     */
    public RouteTester(final GatewayInspector inspector) {
        this.inspector = inspector;
    }

    /**
     * Teste une requête contre toutes les routes de la Gateway.
     *
     * @param request requête à tester, préalablement validée par {@link TestRequest#validate()}
     * @return l'évaluation de chaque route et la simulation de la route retenue
     */
    public Mono<TestResult> test(final TestRequest request) {
        return Mono.zip(inspector.rawRoutes().collectList(), inspector.definitions())
                .flatMap(tuple -> evaluateAll(tuple.getT1(), request)
                        .map(evaluated -> toResult(request, evaluated, tuple.getT2())));
    }

    /**
     * Teste une requête contre des routes absentes de la Gateway (routes en cours d'édition, ADR 0011).
     *
     * @param request requête à tester, préalablement validée par {@link TestRequest#validate()}
     * @param routes routes construites par la Gateway, dans l'ordre d'évaluation
     * @param definitions définitions des routes par identifiant, pour rejouer leurs filtres
     * @return l'évaluation de chaque route et la simulation de la route retenue
     */
    public Mono<TestResult> test(final TestRequest request, final List<Route> routes, final Map<String, RouteDefinition> definitions) {
        return evaluateAll(routes, request).map(evaluated -> toResult(request, evaluated, definitions));
    }

    private Mono<List<Evaluated>> evaluateAll(final List<Route> routes, final TestRequest request) {
        return Flux.fromIterable(routes).concatMap(route -> evaluate(route, request)).collectList();
    }

    private Mono<Evaluated> evaluate(final Route route, final TestRequest request) {
        final ServerWebExchange exchange = SimulatedExchange.create(request.httpMethod(), request.uri(),
                request.httpHeaders(), request.inetRemoteAddress());
        // Comme RoutePredicateHandlerMapping : certains prédicats lisent la route en cours d'évaluation
        exchange.getAttributes().put(GATEWAY_PREDICATE_ROUTE_ATTR, route.getId());
        return Mono.defer(() -> Mono.from(route.getPredicate().apply(exchange)))
                .timeout(PREDICATE_TIMEOUT)
                .map(matched -> new Evaluated(route, exchange, matched ? Outcome.MATCH : Outcome.NO_MATCH, null))
                .defaultIfEmpty(new Evaluated(route, exchange, Outcome.NO_MATCH, null))
                .onErrorResume(ex -> Mono.just(new Evaluated(route, exchange, Outcome.ERROR, describe(ex))));
    }

    private TestResult toResult(final TestRequest request, final List<Evaluated> evaluated,
            final Map<String, RouteDefinition> definitions) {
        final Evaluated selected = evaluated.stream().filter(e -> e.outcome() == Outcome.MATCH).findFirst().orElse(null);
        final List<Evaluation> evaluations = evaluated.stream()
                .map(e -> toEvaluation(e, e == selected, definitions.get(e.route().getId())))
                .toList();
        if (selected == null) {
            return new TestResult(request, request.uri().toString(), evaluations, null, null);
        }
        final Simulation simulation = FilterSimulator.simulate(selected.route(),
                definitions.get(selected.route().getId()), inspector.defaultFilters(), selected.exchange());
        return new TestResult(request, request.uri().toString(), evaluations, selected.route().getId(), simulation);
    }

    private static Evaluation toEvaluation(final Evaluated evaluated, final boolean selected,
            final RouteDefinition definition) {
        final String predicate = definition != null
                ? Definition.summary(definition.getPredicates().stream().map(Definition::of).toList())
                : String.valueOf(evaluated.route().getPredicate());
        final boolean shadowed = evaluated.outcome() == Outcome.MATCH && !selected;
        return new Evaluation(evaluated.route().getId(), evaluated.route().getOrder(), predicate, evaluated.outcome(),
                evaluated.error(), selected, shadowed);
    }

    private static String describe(final Throwable ex) {
        final String message = ex.getMessage();
        return ex.getClass().getSimpleName() + (message != null ? " : " + message : "");
    }

    /** Résultat brut de l'évaluation d'une route, avec l'échange qui porte les variables extraites. */
    private record Evaluated(Route route, ServerWebExchange exchange, Outcome outcome, String error) {
    }
}
