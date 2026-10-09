package dev.gatewayui.inspect;

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
 * Lecture seule de la configuration effective de la Gateway.
 */
public class GatewayInspector {

    private final RouteLocator routeLocator;

    private final RouteDefinitionLocator routeDefinitionLocator;

    private final List<FilterStep> globalFilters;

    private final List<FilterDefinition> defaultFilters;

    public GatewayInspector(RouteLocator routeLocator, RouteDefinitionLocator routeDefinitionLocator,
            List<GlobalFilter> globalFilters, Map<String, GlobalFilter> globalFilterBeans,
            GatewayProperties gatewayProperties) {
        this.routeLocator = routeLocator;
        this.routeDefinitionLocator = routeDefinitionLocator;
        Map<GlobalFilter, String> beanNames = new IdentityHashMap<>();
        globalFilterBeans.forEach((name, filter) -> beanNames.put(filter, name));
        // Même ordre de départ que FilteringWebHandler : le tri stable donnera la même chaîne
        this.globalFilters = globalFilters.stream().map(filter -> FilterStep.ofGlobal(filter, beanNames.get(filter))).toList();
        this.defaultFilters = gatewayProperties != null ? List.copyOf(gatewayProperties.getDefaultFilters()) : List.of();
    }

    /** Routes effectives, dans l'ordre où la Gateway les évalue. */
    public Flux<Route> rawRoutes() {
        return routeLocator.getRoutes();
    }

    public Mono<Map<String, RouteDefinition>> definitions() {
        return routeDefinitionLocator.getRouteDefinitions()
                .filter(definition -> definition.getId() != null)
                .collectMap(RouteDefinition::getId);
    }

    public Mono<List<RouteView>> routes() {
        return Mono.zip(rawRoutes().collectList(), definitions())
                .map(tuple -> tuple.getT1().stream().map(route -> toView(route, tuple.getT2().get(route.getId()))).toList());
    }

    public Mono<List<RouteView>> routes(String query) {
        if (query == null || query.isBlank()) {
            return routes();
        }
        return routes().map(routes -> routes.stream().filter(route -> route.matches(query.trim())).toList());
    }

    public Mono<RouteView> route(String id) {
        return routes().flatMap(routes -> Mono.justOrEmpty(routes.stream().filter(route -> route.id().equals(id)).findFirst()));
    }

    public List<FilterStep> globalFilters() {
        return globalFilters.stream().sorted(Comparator.comparingInt(FilterStep::order)).toList();
    }

    public List<FilterDefinition> defaultFilters() {
        return defaultFilters;
    }

    /**
     * Chaîne complète exécutée pour une route : filtres globaux et filtres de route triés par ordre,
     * exactement comme {@code FilteringWebHandler} (tri stable, filtres globaux en premier à ordre égal).
     */
    public List<FilterStep> pipeline(RouteView route) {
        List<FilterStep> combined = new ArrayList<>(globalFilters);
        combined.addAll(route.filters());
        combined.sort(Comparator.comparingInt(FilterStep::order));
        return combined;
    }

    private RouteView toView(Route route, RouteDefinition definition) {
        List<Definition> predicates = definition == null ? List.of()
                : definition.getPredicates().stream().map(Definition::of).toList();
        List<Definition> filters = definition == null ? List.of()
                : definition.getFilters().stream().map(Definition::of).toList();
        return new RouteView(route.getId(), route.getUri(), route.getOrder(), String.valueOf(route.getPredicate()),
                predicates, filters, route.getFilters().stream().map(FilterStep::ofRoute).toList(),
                new LinkedHashMap<>(route.getMetadata()), definition != null);
    }
}
