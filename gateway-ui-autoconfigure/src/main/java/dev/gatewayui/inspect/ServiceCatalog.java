package dev.gatewayui.inspect;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.gatewayui.autoconfigure.GatewayUiProperties;
import reactor.core.publisher.Mono;

/**
 * Regroupe les routes par service cible.
 * <p>
 * Les services déclarés dans {@code gateway.ui.services} sont rattachés aux routes qui pointent vers leur URL
 * (ou listées dans {@code route-ids}). Les autres cibles HTTP sont déduites des URI des routes si
 * {@code gateway.ui.discover-services} est actif.
 */
public class ServiceCatalog {

    private static final Set<String> SERVICE_SCHEMES = Set.of("http", "https", "ws", "wss", "lb");

    private final GatewayInspector inspector;

    private final GatewayUiProperties properties;

    public ServiceCatalog(GatewayInspector inspector, GatewayUiProperties properties) {
        this.inspector = inspector;
        this.properties = properties;
    }

    public Mono<List<ServiceView>> services() {
        return inspector.routes().map(this::group);
    }

    List<ServiceView> group(List<RouteView> routes) {
        List<ServiceView> services = new ArrayList<>();
        Set<String> assigned = new HashSet<>();

        properties.services().forEach((name, declared) -> {
            String baseUrl = declared.url() != null ? baseUrl(declared.url()) : null;
            List<RouteView> serviceRoutes = routes.stream()
                    .filter(route -> declared.routeIds().contains(route.id())
                            || (baseUrl != null && baseUrl.equals(baseUrl(route.uri()))))
                    .toList();
            serviceRoutes.forEach(route -> assigned.add(route.id()));
            String displayName = declared.displayName() != null ? declared.displayName() : name;
            services.add(new ServiceView(name, displayName, baseUrl, true, serviceRoutes));
        });

        if (properties.discoverServices()) {
            Map<String, List<RouteView>> discovered = new LinkedHashMap<>();
            routes.stream()
                    .filter(route -> !assigned.contains(route.id()))
                    .filter(route -> SERVICE_SCHEMES.contains(route.scheme()) && route.uri().getHost() != null)
                    .forEach(route -> discovered.computeIfAbsent(baseUrl(route.uri()), key -> new ArrayList<>()).add(route));
            Set<String> names = new HashSet<>(properties.services().keySet());
            discovered.forEach((baseUrl, serviceRoutes) -> {
                String name = uniqueName(serviceRoutes.get(0).uri(), names);
                services.add(new ServiceView(name, name, baseUrl, false, serviceRoutes));
            });
        }
        return services;
    }

    /** {@code scheme://host:port}, port par défaut explicite : seuls ces éléments de l'URI servent au routage. */
    static String baseUrl(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        if (port == -1) {
            port = switch (scheme) {
                case "http", "ws" -> 80;
                case "https", "wss" -> 443;
                default -> -1;
            };
        }
        return scheme + "://" + host + (port != -1 ? ":" + port : "");
    }

    /** Nom court tiré du host : {@code orders.shop.svc.cluster.local} devient {@code orders}. */
    private static String uniqueName(URI uri, Set<String> taken) {
        String host = uri.getHost();
        int dot = host.indexOf('.');
        String name = dot > 0 && !Character.isDigit(host.charAt(0)) ? host.substring(0, dot) : host;
        if (!taken.add(name)) {
            name = name + ":" + uri.getPort();
            int suffix = 2;
            while (!taken.add(name)) {
                name = host + " (" + suffix++ + ")";
            }
        }
        return name;
    }
}
