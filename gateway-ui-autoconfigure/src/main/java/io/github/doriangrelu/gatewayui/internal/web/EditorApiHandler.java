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

import io.github.doriangrelu.gatewayui.internal.editor.EditorService;
import io.github.doriangrelu.gatewayui.internal.editor.EditorService.YamlRequest;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * API JSON de l'éditeur graphique (ADR 0011), sous {@code {base-path}/api/editor}.
 *
 * <p>Elle lit la configuration de la Gateway et calcule des résultats (YAML) : elle ne modifie jamais rien.
 */
public class EditorApiHandler {

    private static final MediaType YAML = new MediaType("application", "yaml", StandardCharsets.UTF_8);

    private final EditorService editor;

    /**
     * Crée le handler.
     *
     * @param editor service de l'éditeur
     */
    public EditorApiHandler(final EditorService editor) {
        this.editor = editor;
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
}
