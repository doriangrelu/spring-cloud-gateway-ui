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

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * Cookies qui mémorisent les préférences choisies dans l'UI (langue, thème).
 *
 * <p>Limités au chemin de l'UI, inaccessibles au JavaScript, conservés un an.
 */
class PreferenceCookies {

    private static final Duration DURATION = Duration.ofDays(365);

    private PreferenceCookies() {
    }

    /**
     * Valeur mémorisée d'une préférence.
     *
     * @param request requête HTTP
     * @param name nom du cookie
     * @return la valeur, ou vide si la préférence n'a jamais été choisie
     */
    static Optional<String> read(final ServerRequest request, final String name) {
        final HttpCookie cookie = request.cookies().getFirst(name);
        return Optional.ofNullable(cookie).map(HttpCookie::getValue);
    }

    /**
     * Mémorise une préférence dans un cookie de la réponse.
     *
     * @param request requête HTTP, dont la réponse reçoit le cookie
     * @param name nom du cookie
     * @param value valeur choisie
     * @param path chemin de l'UI, auquel le cookie est limité
     */
    static void remember(final ServerRequest request, final String name, final String value, final String path) {
        request.exchange().getResponse().addCookie(ResponseCookie.from(name, value)
                .path(path)
                .maxAge(DURATION)
                .httpOnly(true)
                .sameSite("Lax")
                .build());
    }
}
