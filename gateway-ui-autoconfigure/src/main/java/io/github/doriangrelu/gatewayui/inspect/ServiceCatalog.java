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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.github.doriangrelu.gatewayui.autoconfigure.GatewayUiProperties;
import reactor.core.publisher.Mono;

/**
 * Regroupe les routes par service cible.
 *
 * <p>Les services déclarés dans {@code gateway.ui.services} sont rattachés aux routes qui pointent vers leur URL ou
 * qui sont listées dans leurs {@code route-ids}. Les autres cibles HTTP sont déduites des URI des routes si
 * {@code gateway.ui.discover-services} est actif.
 */
public class ServiceCatalog {

    private static final Set<String> SERVICE_SCHEMES = Set.of("http", "https", "ws", "wss", "lb");

    private static final int HTTP_PORT = 80;

    private static final int HTTPS_PORT = 443;

    private final GatewayInspector inspector;

    private final GatewayUiProperties properties;

    /**
     * Crée un catalogue.
     *
     * @param inspector inspecteur de la Gateway
     * @param properties configuration de l'UI
     */
    public ServiceCatalog(final GatewayInspector inspector, final GatewayUiProperties properties) {
        this.inspector = inspector;
        this.properties = properties;
    }

    /**
     * Services déclarés, puis services déduits des routes.
     *
     * @return les services et leurs routes
     */
    public Mono<List<ServiceView>> services() {
        return inspector.routes().map(this::group);
    }

    /**
     * Regroupe des routes par service.
     *
     * @param routes routes de la Gateway, dans l'ordre d'évaluation
     * @return les services déclarés puis les services déduits
     */
    List<ServiceView> group(final List<RouteView> routes) {
        final Set<String> assignedRouteIds = new HashSet<>();
        final List<ServiceView> services = new ArrayList<>(declaredServices(routes, assignedRouteIds));
        if (properties.discoverServices()) {
            services.addAll(discoveredServices(routes, assignedRouteIds));
        }
        return services;
    }

    /**
     * Clé de regroupement d'une URI : {@code scheme://host:port}, avec le port par défaut explicite. Seuls ces
     * éléments servent au routage, le chemin de l'URI d'une route est ignoré par la Gateway.
     *
     * @param uri URI d'une route ou d'un service
     * @return l'URL de base normalisée
     */
    static String baseUrl(final URI uri) {
        final String scheme = lowerCase(uri.getScheme());
        final int port = uri.getPort() != -1 ? uri.getPort() : defaultPort(scheme);
        return scheme + "://" + lowerCase(uri.getHost()) + (port != -1 ? ":" + port : "");
    }

    private List<ServiceView> declaredServices(final List<RouteView> routes, final Set<String> assignedRouteIds) {
        final List<ServiceView> services = new ArrayList<>();
        properties.services().forEach((name, declared) -> {
            final ServiceView service = declaredService(name, declared, routes);
            service.routes().forEach(route -> assignedRouteIds.add(route.id()));
            services.add(service);
        });
        return services;
    }

    private static ServiceView declaredService(final String name, final GatewayUiProperties.Service declared,
            final List<RouteView> routes) {
        final String baseUrl = declared.url() != null ? baseUrl(declared.url()) : null;
        final List<RouteView> serviceRoutes = routes.stream()
                .filter(route -> declared.routeIds().contains(route.id()) || baseUrl(route.uri()).equals(baseUrl))
                .toList();
        final String displayName = declared.displayName() != null ? declared.displayName() : name;
        return new ServiceView(name, displayName, baseUrl, true, serviceRoutes);
    }

    private List<ServiceView> discoveredServices(final List<RouteView> routes, final Set<String> assignedRouteIds) {
        final Map<String, List<RouteView>> routesByBaseUrl = new LinkedHashMap<>();
        routes.stream()
                .filter(route -> !assignedRouteIds.contains(route.id()))
                .filter(ServiceCatalog::targetsAService)
                .forEach(route -> routesByBaseUrl.computeIfAbsent(baseUrl(route.uri()), key -> new ArrayList<>()).add(route));
        final Set<String> takenNames = new HashSet<>(properties.services().keySet());
        return routesByBaseUrl.entrySet().stream()
                .map(entry -> discoveredService(entry.getKey(), entry.getValue(), takenNames))
                .toList();
    }

    private static ServiceView discoveredService(final String baseUrl, final List<RouteView> routes,
            final Set<String> takenNames) {
        final String name = uniqueName(routes.getFirst().uri().getHost(), baseUrl, takenNames);
        return new ServiceView(name, name, baseUrl, false, routes);
    }

    private static boolean targetsAService(final RouteView route) {
        return SERVICE_SCHEMES.contains(route.scheme()) && route.uri().getHost() != null;
    }

    /** Nom court tiré du host : {@code orders.shop.svc.cluster.local} devient {@code orders}. */
    private static String uniqueName(final String host, final String baseUrl, final Set<String> takenNames) {
        final int dot = host.indexOf('.');
        final String shortName = dot > 0 && !Character.isDigit(host.charAt(0)) ? host.substring(0, dot) : host;
        return takenNames.add(shortName) ? shortName : baseUrl;
    }

    private static int defaultPort(final String scheme) {
        return switch (scheme) {
            case "http", "ws" -> HTTP_PORT;
            case "https", "wss" -> HTTPS_PORT;
            default -> -1;
        };
    }

    private static String lowerCase(final String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
