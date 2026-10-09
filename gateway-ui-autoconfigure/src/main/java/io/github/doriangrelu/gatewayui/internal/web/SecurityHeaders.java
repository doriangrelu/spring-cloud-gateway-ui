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

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * En-têtes de sécurité HTTP ajoutés aux réponses de l'UI, et uniquement à elles : le filtre est posé sur la
 * {@code RouterFunction} de l'UI, les routes de la Gateway ne sont jamais concernées.
 *
 * <p>La politique de contenu n'autorise que les ressources servies par l'UI elle-même : aucun script ni style inline,
 * aucune ressource externe, aucune intégration dans une frame.
 */
public class SecurityHeaders {

    /** Politique de contenu de l'UI. Les appels htmx sont des requêtes vers l'UI ({@code connect-src 'self'}). */
    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'none'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "form-action 'self'",
            "base-uri 'none'",
            "frame-ancestors 'none'");

    private static final Map<String, String> HEADERS = Map.of(
            "Content-Security-Policy", CONTENT_SECURITY_POLICY,
            "X-Content-Type-Options", "nosniff",
            "X-Frame-Options", "DENY",
            "Referrer-Policy", "no-referrer");

    private SecurityHeaders() {
    }

    /**
     * Filtre qui pose les en-têtes de sécurité sur la réponse avant de déléguer au handler de l'UI.
     *
     * <p>Les en-têtes sont posés sur la réponse de l'échange : un {@link ServerResponse} y ajoute les siens à
     * l'écriture, sans retirer ceux-ci.
     *
     * @return le filtre à poser sur la fonction de routage de l'UI
     */
    public static HandlerFilterFunction<ServerResponse, ServerResponse> filter() {
        return (request, next) -> {
            final HttpHeaders headers = request.exchange().getResponse().getHeaders();
            HEADERS.forEach(headers::set);
            return next.handle(request);
        };
    }
}
