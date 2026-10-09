package dev.gatewayui.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Données communes à toutes les pages : chemins de l'UI et onglet actif.
 */
public record UiContext(String basePath, String page) {

    public String url(String path) {
        return basePath + path;
    }

    public String routeUrl(String routeId) {
        return basePath + "/routes/" + URLEncoder.encode(routeId, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public String asset(String name) {
        return basePath + "/assets/" + name;
    }

    public String navClass(String target) {
        return target.equals(page) ? "nav-link active" : "nav-link";
    }

    UiContext page(String newPage) {
        return new UiContext(basePath, newPage);
    }
}
