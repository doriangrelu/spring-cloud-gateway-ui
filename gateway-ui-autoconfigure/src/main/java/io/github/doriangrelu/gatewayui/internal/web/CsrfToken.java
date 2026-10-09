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

import java.util.Optional;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * Jeton CSRF de Spring Security, quand l'application hôte active la protection : l'éditeur le renvoie dans ses appels
 * POST (ADR 0011).
 *
 * <p>Spring Security n'est pas une dépendance du starter : le jeton est lu par ses propriétés, sous l'attribut d'échange
 * où le {@code CsrfWebFilter} de Spring Security le dépose.
 *
 * @param headerName en-tête HTTP qui porte le jeton
 * @param token valeur du jeton
 */
public record CsrfToken(String headerName, String token) {

    /** Attribut d'échange de Spring Security : {@code CsrfToken.class.getName()}, valeur {@code Mono<CsrfToken>}. */
    static final String ATTRIBUTE = "org.springframework.security.web.server.csrf.CsrfToken";

    /**
     * Jeton CSRF de la requête.
     *
     * <p>Le jeton de Spring Security est paresseux : c'est cette lecture qui le crée et l'enregistre (en session par
     * défaut), pour que les appels POST suivants soient acceptés.
     *
     * @param request requête HTTP de la page
     * @return le jeton, ou vide si la protection CSRF n'est pas active
     */
    static Mono<Optional<CsrfToken>> of(final ServerRequest request) {
        final Object token = request.exchange().getAttribute(ATTRIBUTE);
        if (!(token instanceof Mono<?> mono)) {
            return Mono.just(Optional.empty());
        }
        return mono.map(CsrfToken::read).defaultIfEmpty(Optional.empty());
    }

    private static Optional<CsrfToken> read(final Object token) {
        final BeanWrapper properties = new BeanWrapperImpl(token);
        return Optional.of(new CsrfToken(String.valueOf(properties.getPropertyValue("headerName")),
                String.valueOf(properties.getPropertyValue("token"))));
    }
}
