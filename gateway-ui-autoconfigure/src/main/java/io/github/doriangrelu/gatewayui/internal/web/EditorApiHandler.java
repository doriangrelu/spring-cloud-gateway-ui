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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.editor.Catalog;
import io.github.doriangrelu.gatewayui.internal.editor.EditableRoute;
import io.github.doriangrelu.gatewayui.internal.editor.EditorService;
import io.github.doriangrelu.gatewayui.internal.editor.EditorService.AdviceRequest;
import io.github.doriangrelu.gatewayui.internal.editor.EditorService.YamlRequest;
import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;
import io.github.doriangrelu.gatewayui.internal.tester.TestResult;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * API JSON de l'éditeur graphique (ADR 0011), sous {@code {base-path}/api/editor}.
 *
 * <p>Elle lit la configuration de la Gateway et calcule des résultats (YAML, conseils, simulation) : elle ne modifie jamais rien.
 */
public class EditorApiHandler {

    private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private static final MediaType YAML = new MediaType("application", "yaml", StandardCharsets.UTF_8);

    private final EditorService editor;

    private final UiContexts contexts;

    private final Catalog catalog;

    private final TemplateRenderer renderer;

    /**
     * Crée le handler.
     *
     * @param editor service de l'éditeur
     * @param contexts contextes de rendu, pour la langue des textes renvoyés
     * @param catalog catalogue des prédicats et filtres
     * @param renderer moteur de templates, pour le résultat du testeur
     */
    public EditorApiHandler(final EditorService editor, final UiContexts contexts, final Catalog catalog,
            final TemplateRenderer renderer) {
        this.renderer = renderer;
        this.editor = editor;
        this.contexts = contexts;
        this.catalog = catalog;
    }

    /**
     * Routes déclarées de la Gateway, éditables.
     *
     * @param request requête HTTP
     * @return les routes, en JSON
     */
    public Mono<ServerResponse> routes(final ServerRequest request) {
        return editor.routes().flatMap(routes -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(routes));
    }

    /**
     * Identifiants des routes déclarées en Java, non éditables.
     *
     * @param request requête HTTP
     * @return les identifiants, en JSON
     */
    public Mono<ServerResponse> javaRoutes(final ServerRequest request) {
        return editor.javaRoutes().flatMap(ids -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(ids));
    }

    /**
     * Fabriques de prédicats et de filtres de la Gateway.
     *
     * @param request requête HTTP
     * @return les fabriques, en JSON
     */
    public Mono<ServerResponse> factories(final ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(editor.factories());
    }

    /**
     * Catalogue des prédicats et filtres, textes traduits : aide de la palette et du panneau (ADR 0013).
     *
     * @param request requête HTTP
     * @return les fabriques, en JSON
     */
    public Mono<ServerResponse> catalog(final ServerRequest request) {
        final UiContext ui = contexts.create(request, "editor");
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .bodyValue(catalog.entries().stream().map(entry -> CatalogView.of(entry, ui)).toList());
    }

    /**
     * Teste une requête contre la route éditée (ADR 0011) : la route est construite par la Gateway sans lui être ajoutée.
     *
     * @param request requête HTTP, avec la route éditée et la requête à tester en JSON
     * @return le résultat, textes traduits
     */
    public Mono<ServerResponse> simulate(final ServerRequest request) {
        final UiContext ui = contexts.create(request, "editor");
        return request.bodyToMono(SimulationRequest.class)
                .flatMap(body -> simulate(body, ui))
                .flatMap(view -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(view));
    }

    private Mono<EditorSimulationView> simulate(final SimulationRequest body, final UiContext ui) {
        final TestRequest test = new TestRequest(body.method(), body.path(), body.host(), body.headers(), null);
        final Message invalid = test.validate();
        if (invalid != null) {
            return Mono.just(EditorSimulationView.failure(invalid, ui));
        }
        return editor.simulate(body.route(), test).map(outcome -> EditorSimulationView.of(outcome, ui));
    }

    /**
     * Testeur de l'éditeur : même formulaire et même résultat que la page « Testeur », appliqués à la route éditée, seule
     * ou dans la configuration de l'espace de travail. Renvoie le fragment HTML du résultat du testeur.
     *
     * @param request requête HTTP, avec la route éditée, les autres routes et la requête à tester en JSON
     * @return le fragment HTML du résultat, textes traduits
     */
    public Mono<ServerResponse> test(final ServerRequest request) {
        final UiContext ui = contexts.create(request, "editor");
        return request.bodyToMono(TesterRequest.class)
                .flatMap(body -> test(body, ui))
                .flatMap(html -> ServerResponse.ok().contentType(HTML).bodyValue(html));
    }

    private Mono<String> test(final TesterRequest body, final UiContext ui) {
        final TestRequest test = new TestRequest(body.method(), body.path(), body.host(), body.headers(), body.remoteAddress());
        final Message invalid = test.validate();
        if (invalid != null) {
            return Mono.just(testerResult(ui, test, null, invalid));
        }
        final List<EditableRoute> others = body.context() && body.routes() != null ? body.routes() : List.of();
        return editor.simulate(body.route(), others, test)
                .map(outcome -> testerResult(ui, test, outcome.result(), outcome.error()));
    }

    private String testerResult(final UiContext ui, final TestRequest request, final TestResult result, final Message error) {
        // HashMap : le template attend des paramètres présents, même nuls
        final Map<String, Object> params = new HashMap<>();
        params.put("ui", ui);
        params.put("request", request);
        params.put("result", result);
        params.put("error", error);
        params.put("editor", true);
        return renderer.render("testerResult", params);
    }

    /**
     * Conseils sur la route éditée, dans le contexte des autres routes de l'espace de travail.
     *
     * @param request requête HTTP, avec la route éditée et les autres routes en JSON
     * @return les conseils, textes traduits
     */
    public Mono<ServerResponse> advice(final ServerRequest request) {
        final UiContext ui = contexts.create(request, "editor");
        return request.bodyToMono(AdviceRequest.class)
                .map(body -> editor.advise(body).stream().map(advice -> EditorAdviceView.of(advice, ui)).toList())
                .flatMap(views -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(views));
    }

    /**
     * YAML des routes envoyées.
     *
     * @param request requête HTTP, avec les routes et les commentaires en JSON
     * @return le document YAML
     */
    public Mono<ServerResponse> yaml(final ServerRequest request) {
        return request.bodyToMono(YamlRequest.class)
                .map(editor::yaml)
                .flatMap(document -> ServerResponse.ok().contentType(YAML).bodyValue(document));
    }

    /**
     * Demande de simulation de la route éditée.
     *
     * @param route route éditée
     * @param method méthode HTTP de la requête à tester
     * @param path chemin et query string
     * @param host hôte de la requête
     * @param headers en-têtes, un par ligne
     */
    public record SimulationRequest(EditableRoute route, String method, String path, String host, String headers) {
    }

    /**
     * Demande de test de la route éditée, avec les champs de la page « Testeur ».
     *
     * @param route route éditée
     * @param routes autres routes de l'espace de travail, hors routes retirées
     * @param context évaluer la route éditée dans la configuration, avec les autres routes
     * @param method méthode HTTP de la requête à tester
     * @param path chemin et query string
     * @param host hôte de la requête
     * @param headers en-têtes, un par ligne
     * @param remoteAddress adresse du client
     */
    public record TesterRequest(EditableRoute route, List<EditableRoute> routes, boolean context, String method, String path,
            String host, String headers, String remoteAddress) {
    }
}
