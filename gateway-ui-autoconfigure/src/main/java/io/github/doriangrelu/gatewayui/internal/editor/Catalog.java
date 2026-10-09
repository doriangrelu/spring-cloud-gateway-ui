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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.support.ShortcutConfigurable;

/**
 * Catalogue des prédicats et filtres de la Gateway (ADR 0013) : catégorie, champs et lien vers la documentation
 * officielle de chaque fabrique présente dans l'application, y compris les fabriques maison.
 */
public class Catalog {

    private static final String REFERENCE = "https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/";

    private static final String PREDICATES_PAGE = REFERENCE + "request-predicates-factories.html";

    private static final String FILTERS_PAGE = REFERENCE + "gatewayfilter-factories.html";

    private static final String GATEWAY_PACKAGE = "org.springframework.cloud.gateway.";

    private static final String OTHER = "other";

    private static final String CUSTOM = "custom";

    /** Catégories des prédicats, dans l'ordre d'affichage. */
    private static final Map<String, List<String>> PREDICATE_CATEGORIES = ordered(List.of(
            Map.entry("path-host", List.of("Path", "Host")),
            Map.entry("request", List.of("Method", "Header", "Query", "Cookie", "ReadBody", "Version")),
            Map.entry("date", List.of("After", "Before", "Between")),
            Map.entry("client", List.of("RemoteAddr", "XForwardedRemoteAddr")),
            Map.entry("distribution", List.of("Weight", "CloudFoundryRouteService"))));

    /** Catégories des filtres, dans l'ordre d'affichage. */
    private static final Map<String, List<String>> FILTER_CATEGORIES = ordered(List.of(
            Map.entry("path", List.of("StripPrefix", "PrefixPath", "RewritePath", "SetPath", "SetRequestUri")),
            Map.entry("request-headers", List.of("AddRequestHeader", "AddRequestHeadersIfNotPresent", "SetRequestHeader",
                    "RemoveRequestHeader",
                    "MapRequestHeader", "PreserveHostHeader", "SetRequestHostHeader", "RequestHeaderToRequestUri", "RequestHeaderSize")),
            Map.entry("response-headers", List.of("AddResponseHeader", "SetResponseHeader", "RemoveResponseHeader", "RewriteResponseHeader",
                    "DedupeResponseHeader", "RewriteLocationResponseHeader", "SecureHeaders")),
            Map.entry("parameters", List.of("AddRequestParameter", "RemoveRequestParameter", "RewriteRequestParameter")),
            Map.entry("resilience", List.of("CircuitBreaker", "Retry", "RequestRateLimiter", "FallbackHeaders", "RequestSize")),
            Map.entry("body", List.of("ModifyRequestBody", "ModifyResponseBody", "CacheRequestBody", "RemoveJsonAttributesResponseBody",
                    "JsonToGrpc")),
            Map.entry("routing", List.of("RedirectTo", "SetStatus", "SaveSession", "TokenRelay", "LocalResponseCache"))));

    /** Pages de la documentation dont le nom ne suit pas la règle {@code <nom>-factory.html}. */
    private static final Map<String, String> FILTER_PAGES = Map.of(
            "CircuitBreaker", "circuitbreaker-filter-factory.html",
            "FallbackHeaders", "fallback-headers.html",
            "LocalResponseCache", "local-cache-response-filter.html");

    /** Ancres de la documentation dont le nom ne suit pas la règle {@code <nom>-route-predicate-factory}. */
    private static final Map<String, String> PREDICATE_ANCHORS = Map.of(
            "XForwardedRemoteAddr", "xforwarded-remote-addr-route-predicate-factory",
            "ReadBody", "read-body-route-predicate-factory");

    /** Fabriques sans section dédiée dans la documentation officielle (vérifié pour la Gateway 5.0) : page générale. */
    private static final Set<String> WITHOUT_SECTION = Set.of("SetRequestUri", "CloudFoundryRouteService");

    private final List<CatalogEntry> entries;

    /**
     * Construit le catalogue des fabriques de la Gateway.
     *
     * @param predicates fabriques de prédicats
     * @param filters fabriques de filtres
     */
    public Catalog(final List<RoutePredicateFactory<?>> predicates, final List<GatewayFilterFactory<?>> filters) {
        this.entries = Stream.concat(
                predicates.stream().map(factory -> entry(CatalogEntry.PREDICATE, factory.name(), factory)),
                filters.stream().map(factory -> entry(CatalogEntry.FILTER, factory.name(), factory)))
                .sorted(Comparator.comparing(CatalogEntry::name))
                .toList();
    }

    /**
     * Fabriques de l'application, triées par nom.
     *
     * @return les fabriques
     */
    public List<CatalogEntry> entries() {
        return entries;
    }

    /**
     * Codes des catégories d'un type de fabrique, dans l'ordre d'affichage, catégories {@code other} et {@code custom}
     * comprises.
     *
     * @param kind {@code predicate} ou {@code filter}
     * @return les codes
     */
    public static List<String> categories(final String kind) {
        return Stream.concat(categoriesOf(kind).keySet().stream(), Stream.of(OTHER, CUSTOM)).toList();
    }

    private static CatalogEntry entry(final String kind, final String name, final ShortcutConfigurable factory) {
        final boolean custom = !factory.getClass().getName().startsWith(GATEWAY_PACKAGE);
        final String category = custom ? CUSTOM : category(kind, name);
        return new CatalogEntry(kind, name, category, factory.shortcutFieldOrder(), custom ? null : documentation(kind, name), custom);
    }

    private static String category(final String kind, final String name) {
        return categoriesOf(kind).entrySet().stream()
                .filter(category -> category.getValue().contains(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(OTHER);
    }

    /** Fabrique inconnue du catalogue (version plus récente de la Gateway) ou sans section dédiée : lien vers la page générale. */
    private static String documentation(final String kind, final String name) {
        final boolean known = !OTHER.equals(category(kind, name)) && !WITHOUT_SECTION.contains(name);
        if (CatalogEntry.PREDICATE.equals(kind)) {
            return known ? PREDICATES_PAGE + "#" + PREDICATE_ANCHORS.getOrDefault(name, lower(name) + "-route-predicate-factory")
                    : PREDICATES_PAGE;
        }
        return known ? REFERENCE + "gatewayfilter-factories/" + FILTER_PAGES.getOrDefault(name, lower(name) + "-factory.html")
                : FILTERS_PAGE;
    }

    private static Map<String, List<String>> categoriesOf(final String kind) {
        return CatalogEntry.PREDICATE.equals(kind) ? PREDICATE_CATEGORIES : FILTER_CATEGORIES;
    }

    private static String lower(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static Map<String, List<String>> ordered(final List<Map.Entry<String, List<String>>> categories) {
        final Map<String, List<String>> ordered = new LinkedHashMap<>();
        categories.forEach(category -> ordered.put(category.getKey(), category.getValue()));
        return ordered;
    }
}
