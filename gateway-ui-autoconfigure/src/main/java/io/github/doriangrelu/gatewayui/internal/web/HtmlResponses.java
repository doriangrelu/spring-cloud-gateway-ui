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
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Réponses HTML communes aux handlers des pages.
 */
class HtmlResponses {

    private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private HtmlResponses() {
    }

    static Mono<ServerResponse> ok(final TemplateRenderer renderer, final String template, final Map<String, Object> params) {
        return ServerResponse.ok().contentType(HTML).bodyValue(renderer.render(template, params));
    }

    static Mono<ServerResponse> notFound(final TemplateRenderer renderer, final UiContext page, final Message message) {
        return ServerResponse.status(HttpStatus.NOT_FOUND).contentType(HTML)
                .bodyValue(renderer.render("notFound", Map.of("ui", page, "message", message)));
    }

    /** Requête htmx, hors restauration d'historique qui attend la page complète. */
    static boolean isHtmx(final ServerRequest request) {
        return "true".equals(request.headers().firstHeader("HX-Request"))
                && !"true".equals(request.headers().firstHeader("HX-History-Restore-Request"));
    }
}
