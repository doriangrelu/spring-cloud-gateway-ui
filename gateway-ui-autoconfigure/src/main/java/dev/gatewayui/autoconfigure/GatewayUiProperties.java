package dev.gatewayui.autoconfigure;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration de l'UI Gateway ({@code gateway.ui.*}).
 *
 * @param enabled active l'UI ; désactivée par défaut pour ne jamais l'exposer par accident
 * @param basePath préfixe sous lequel l'UI et ses ressources sont servies
 * @param discoverServices déduit les services à partir des URI des routes
 * @param services services déclarés explicitement, ou surcharges des services déduits
 */
@ConfigurationProperties(GatewayUiProperties.PREFIX)
public record GatewayUiProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("/gateway-ui") String basePath,
        @DefaultValue("true") boolean discoverServices,
        Map<String, Service> services) {

    public static final String PREFIX = "gateway.ui";

    public GatewayUiProperties {
        basePath = normalizeBasePath(basePath);
        // Conserve l'ordre de déclaration pour l'affichage
        services = services == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(services));
    }

    /**
     * @param url URL de base du service, comparée aux URI des routes
     * @param displayName libellé affiché dans l'UI
     * @param routeIds routes rattachées explicitement au service, en plus de celles qui pointent vers son URL
     */
    public record Service(URI url, String displayName, List<String> routeIds) {

        public Service {
            routeIds = routeIds == null ? List.of() : List.copyOf(routeIds);
        }
    }

    private static String normalizeBasePath(String path) {
        String normalized = path == null || path.isBlank() ? "/gateway-ui" : path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.equals("/")) {
            throw new IllegalArgumentException(PREFIX + ".base-path ne peut pas être la racine : l'UI masquerait les routes de la Gateway");
        }
        return normalized;
    }
}
