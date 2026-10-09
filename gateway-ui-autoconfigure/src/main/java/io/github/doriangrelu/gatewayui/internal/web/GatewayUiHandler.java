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
package io.github.doriangrelu.gatewayui.internal.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.editor.Catalog;
import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.inspect.GatewayInspector;
import io.github.doriangrelu.gatewayui.internal.inspect.ServiceCatalog;
import io.github.doriangrelu.gatewayui.internal.tester.RouteTester;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Pages de l'UI.
 *
 * <p>Une requête htmx (en-tête {@code HX-Request}) reçoit uniquement le fragment à remplacer, une navigation classique
 * reçoit la page complète : chaque écran reste accessible et partageable par son URL.
 */
public class GatewayUiHandler {

    private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final UiContexts contexts;

    private final TemplateRenderer renderer;

    private final GatewayInspector inspector;

    private final ServiceCatalog serviceCatalog;

    private final RouteTester routeTester;

    private final Catalog catalog;

    /**
     * Crée le handler des pages.
     *
     * @param contexts fabrique des contextes de rendu (chemin de l'UI, langue)
     * @param renderer moteur de templates
     * @param inspector inspecteur de la Gateway
     * @param serviceCatalog catalogue des services
     * @param routeTester testeur de routes
     * @param catalog catalogue des prédicats et filtres
     */
    public GatewayUiHandler(final UiContexts contexts, final TemplateRenderer renderer, final GatewayInspector inspector,
            final ServiceCatalog serviceCatalog, final RouteTester routeTester, final Catalog catalog) {
        this.contexts = contexts;
        this.renderer = renderer;
        this.inspector = inspector;
        this.serviceCatalog = serviceCatalog;
        this.routeTester = routeTester;
        this.catalog = catalog;
    }

    /**
     * Racine de l'UI : redirige vers la liste des routes.
     *
     * @param request requête HTTP
     * @return une redirection
     */
    public Mono<ServerResponse> home(final ServerRequest request) {
        return ServerResponse.temporaryRedirect(URI.create(contexts.basePath() + "/routes")).build();
    }

    /**
     * Liste des routes, filtrée par le paramètre {@code q}.
     *
     * @param request requête HTTP
     * @return la page, ou le seul tableau pour une recherche htmx
     */
    public Mono<ServerResponse> routes(final ServerRequest request) {
        final String query = request.queryParam("q").orElse("");
        final UiContext page = contexts.create(request, "routes");
        final String template = isHtmx(request) ? "routesTable" : "routes";
        return inspector.routes(query)
                .flatMap(routes -> html(template, Map.of("ui", page, "routes", routes, "query", query)));
    }

    /**
     * Détail d'une route.
     *
     * @param request requête HTTP, avec la variable de chemin {@code id}
     * @return la page, ou une page 404 si la route n'existe pas
     */
    public Mono<ServerResponse> route(final ServerRequest request) {
        final UiContext page = contexts.create(request, "routes");
        final String id = request.pathVariable("id");
        return inspector.route(id)
                .flatMap(route -> html("route", Map.of("ui", page, "route", route, "pipeline", inspector.pipeline(route))))
                .switchIfEmpty(Mono.defer(() -> notFound(page, Message.of("route.notFound", id))));
    }

    /**
     * Services cibles et routes qui y mènent.
     *
     * @param request requête HTTP
     * @return la page
     */
    public Mono<ServerResponse> services(final ServerRequest request) {
        final UiContext page = contexts.create(request, "services");
        return serviceCatalog.services().flatMap(services -> html("services", Map.of("ui", page, "services", services)));
    }

    /**
     * Filtres globaux.
     *
     * @param request requête HTTP
     * @return la page
     */
    public Mono<ServerResponse> globalFilters(final ServerRequest request) {
        return html("globalFilters", Map.of("ui", contexts.create(request, "filters"), "filters", inspector.globalFilters()));
    }

    /**
     * Testeur de routes. Sans chemin saisi, affiche le formulaire vide.
     *
     * @param request requête HTTP, avec les paramètres du formulaire
     * @return la page, ou le seul résultat pour une soumission htmx
     */
    public Mono<ServerResponse> tester(final ServerRequest request) {
        final TestRequest testRequest = TestRequest.from(request.queryParams());
        final String template = isHtmx(request) ? "testerResult" : "tester";
        final Map<String, Object> params = testerParams(contexts.create(request, "tester"), testRequest);
        if (TestRequest.isEmpty(request.queryParams())) {
            return html(template, params);
        }
        final Message error = testRequest.validate();
        if (error != null) {
            params.put("error", error);
            return html(template, params);
        }
        return routeTester.test(testRequest).flatMap(result -> {
            params.put("result", result);
            return html(template, params);
        });
    }

    /**
     * Éditeur graphique de routes (ADR 0011) : la page fournit la structure, les textes traduits et le jeton CSRF ; les
     * interactions sont assurées par ses scripts, branchés sur l'API de l'éditeur.
     *
     * @param request requête HTTP
     * @return la page
     */
    public Mono<ServerResponse> editor(final ServerRequest request) {
        final UiContext page = contexts.create(request, "editor");
        return CsrfToken.of(request).flatMap(csrf -> html("editor", Map.of("ui", page, "csrf", csrf)));
    }

    /**
     * Catalogue documenté des prédicats et filtres (ADR 0013).
     *
     * @param request requête HTTP
     * @return la page
     */
    public Mono<ServerResponse> catalog(final ServerRequest request) {
        final UiContext page = contexts.create(request, "catalog");
        return html("catalog", Map.of("ui", page, "groups", CatalogView.groups(catalog.entries(), page)));
    }

    private static Map<String, Object> testerParams(final UiContext page, final TestRequest testRequest) {
        // HashMap : les templates attendent des paramètres présents, même nuls
        final Map<String, Object> params = new HashMap<>();
        params.put("ui", page);
        params.put("request", testRequest);
        params.put("result", null);
        params.put("error", null);
        return params;
    }

    private Mono<ServerResponse> notFound(final UiContext page, final Message message) {
        return ServerResponse.status(HttpStatus.NOT_FOUND).contentType(HTML)
                .bodyValue(renderer.render("notFound", Map.of("ui", page, "message", message)));
    }

    private Mono<ServerResponse> html(final String template, final Map<String, Object> params) {
        return ServerResponse.ok().contentType(HTML).bodyValue(renderer.render(template, params));
    }

    /** Requête htmx, hors restauration d'historique qui attend la page complète. */
    private static boolean isHtmx(final ServerRequest request) {
        return "true".equals(request.headers().firstHeader("HX-Request"))
                && !"true".equals(request.headers().firstHeader("HX-History-Restore-Request"));
    }
}
