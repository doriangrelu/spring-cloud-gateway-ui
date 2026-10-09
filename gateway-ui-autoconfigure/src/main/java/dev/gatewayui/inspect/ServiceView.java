package dev.gatewayui.inspect;

import java.util.List;

/**
 * Service cible de la Gateway et routes qui y mènent.
 *
 * @param baseUrl {@code scheme://host:port}, ou {@code null} pour un service déclaré uniquement par {@code route-ids}
 * @param declared {@code true} si le service vient de {@code gateway.ui.services}, {@code false} s'il est déduit des routes
 */
public record ServiceView(String name, String displayName, String baseUrl, boolean declared, List<RouteView> routes) {
}
