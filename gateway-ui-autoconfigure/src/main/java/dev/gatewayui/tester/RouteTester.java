package dev.gatewayui.tester;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.gatewayui.inspect.Definition;
import dev.gatewayui.inspect.GatewayInspector;
import dev.gatewayui.tester.TestResult.Evaluation;
import dev.gatewayui.tester.TestResult.Outcome;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR;

/**
 * Indique quelle route recevrait une requête donnée, et ce que la Gateway transmettrait au service.
 * <p>
 * Toutes les routes sont évaluées (et pas seulement jusqu'à la première qui correspond) pour signaler
 * celles qui sont masquées par une route prioritaire.
 */
public class RouteTester {

    private static final Duration PREDICATE_TIMEOUT = Duration.ofSeconds(2);

    private final GatewayInspector inspector;

    public RouteTester(GatewayInspector inspector) {
        this.inspector = inspector;
    }

    public Mono<TestResult> test(TestRequest request) {
        return Mono.zip(inspector.rawRoutes().collectList(), inspector.definitions())
                .flatMap(tuple -> Flux.fromIterable(tuple.getT1())
                        .concatMap(route -> evaluate(route, request))
                        .collectList()
                        .map(evaluated -> toResult(request, evaluated, tuple.getT2())));
    }

    private Mono<Evaluated> evaluate(Route route, TestRequest request) {
        ServerWebExchange exchange = SimulatedExchange.create(request.httpMethod(), request.uri(), request.httpHeaders(),
                request.inetRemoteAddress());
        // Comme RoutePredicateHandlerMapping : certains prédicats lisent la route en cours d'évaluation
        exchange.getAttributes().put(GATEWAY_PREDICATE_ROUTE_ATTR, route.getId());
        return Mono.defer(() -> Mono.from(route.getPredicate().apply(exchange)))
                .timeout(PREDICATE_TIMEOUT)
                .map(matched -> new Evaluated(route, exchange, matched ? Outcome.MATCH : Outcome.NO_MATCH, null))
                .defaultIfEmpty(new Evaluated(route, exchange, Outcome.NO_MATCH, null))
                .onErrorResume(ex -> Mono.just(new Evaluated(route, exchange, Outcome.ERROR, describe(ex))));
    }

    private TestResult toResult(TestRequest request, List<Evaluated> evaluated, Map<String, RouteDefinition> definitions) {
        Evaluated selected = evaluated.stream().filter(e -> e.outcome == Outcome.MATCH).findFirst().orElse(null);
        List<Evaluation> evaluations = new ArrayList<>();
        for (Evaluated e : evaluated) {
            boolean isSelected = e == selected;
            RouteDefinition definition = definitions.get(e.route.getId());
            String predicate = definition != null
                    ? Definition.summary(definition.getPredicates().stream().map(Definition::of).toList())
                    : String.valueOf(e.route.getPredicate());
            evaluations.add(new Evaluation(e.route.getId(), e.route.getOrder(), predicate, e.outcome, e.error,
                    isSelected, e.outcome == Outcome.MATCH && !isSelected));
        }
        TestResult.Simulation simulation = selected == null ? null
                : FilterSimulator.simulate(selected.route, definitions.get(selected.route.getId()),
                        inspector.defaultFilters(), selected.exchange);
        return new TestResult(request, request.uri().toString(), evaluations,
                selected == null ? null : selected.route.getId(), simulation);
    }

    private static String describe(Throwable ex) {
        String message = ex.getMessage();
        return ex.getClass().getSimpleName() + (message != null ? " : " + message : "");
    }

    private record Evaluated(Route route, ServerWebExchange exchange, Outcome outcome, String error) {
    }
}
