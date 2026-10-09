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
package io.github.doriangrelu.gatewayui.tester;

import java.net.InetSocketAddress;
import java.net.URI;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Échange fictif sur lequel on évalue les prédicats des routes, sans jamais émettre de requête réelle.
 *
 * <p>La requête a un corps vide et la réponse ignore tout ce qui y est écrit.
 */
class SimulatedExchange {

    private static final ServerCodecConfigurer CODECS = ServerCodecConfigurer.create();

    private SimulatedExchange() {
    }

    /**
     * Crée un échange fictif.
     *
     * @param method méthode HTTP
     * @param uri URI complète de la requête
     * @param headers en-têtes de la requête (copiés)
     * @param remoteAddress adresse du client
     * @return l'échange
     */
    static ServerWebExchange create(final HttpMethod method, final URI uri, final HttpHeaders headers,
            final InetSocketAddress remoteAddress) {
        return new DefaultServerWebExchange(new Request(method, uri, headers, remoteAddress), new Response(),
                new DefaultWebSessionManager(), CODECS, new AcceptHeaderLocaleContextResolver());
    }

    /** Requête au corps vide, dont les cookies sont lus depuis l'en-tête {@code Cookie}. */
    private static class Request extends AbstractServerHttpRequest {

        private final InetSocketAddress remoteAddress;

        Request(final HttpMethod method, final URI uri, final HttpHeaders headers, final InetSocketAddress remoteAddress) {
            super(method, uri, null, copy(headers));
            this.remoteAddress = remoteAddress;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return remoteAddress;
        }

        @Override
        public Flux<DataBuffer> getBody() {
            return Flux.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getNativeRequest() {
            return (T) this;
        }

        @Override
        protected MultiValueMap<String, HttpCookie> initCookies() {
            final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
            getHeaders().getOrEmpty(HttpHeaders.COOKIE)
                    .forEach(header -> parseCookieHeader(header, cookies));
            return cookies;
        }

        @Override
        protected SslInfo initSslInfo() {
            return null;
        }

        private static void parseCookieHeader(final String header, final MultiValueMap<String, HttpCookie> cookies) {
            for (final String pair : header.split(";")) {
                final int separator = pair.indexOf('=');
                if (separator > 0) {
                    final String name = pair.substring(0, separator).trim();
                    cookies.add(name, new HttpCookie(name, pair.substring(separator + 1).trim()));
                }
            }
        }

        private static HttpHeaders copy(final HttpHeaders headers) {
            final HttpHeaders copy = new HttpHeaders();
            copy.addAll(headers);
            return copy;
        }
    }

    /** Réponse qui consomme et ignore ce qui y est écrit. */
    private static class Response extends AbstractServerHttpResponse {

        Response() {
            super(DefaultDataBufferFactory.sharedInstance);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getNativeResponse() {
            return (T) this;
        }

        @Override
        protected Mono<Void> writeWithInternal(final Publisher<? extends DataBuffer> body) {
            return Flux.from(body).then();
        }

        @Override
        protected Mono<Void> writeAndFlushWithInternal(final Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return Flux.from(body).flatMap(Flux::from).then();
        }

        @Override
        protected void applyStatusCode() {
            // Rien à appliquer : la réponse n'est jamais envoyée
        }

        @Override
        protected void applyHeaders() {
            // Rien à appliquer : la réponse n'est jamais envoyée
        }

        @Override
        protected void applyCookies() {
            // Rien à appliquer : la réponse n'est jamais envoyée
        }
    }
}
