package dev.gatewayui.tester;

import java.net.InetSocketAddress;
import java.net.URI;

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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Échange fictif sur lequel on évalue les prédicats des routes, sans jamais émettre de requête réelle.
 */
final class SimulatedExchange {

    private static final ServerCodecConfigurer CODECS = ServerCodecConfigurer.create();

    private SimulatedExchange() {
    }

    static ServerWebExchange create(HttpMethod method, URI uri, HttpHeaders headers, InetSocketAddress remoteAddress) {
        return new DefaultServerWebExchange(new Request(method, uri, headers, remoteAddress), new Response(),
                new DefaultWebSessionManager(), CODECS, new AcceptHeaderLocaleContextResolver());
    }

    private static final class Request extends AbstractServerHttpRequest {

        private final InetSocketAddress remoteAddress;

        Request(HttpMethod method, URI uri, HttpHeaders headers, InetSocketAddress remoteAddress) {
            super(method, uri, null, copy(headers));
            this.remoteAddress = remoteAddress;
        }

        private static HttpHeaders copy(HttpHeaders headers) {
            HttpHeaders copy = new HttpHeaders();
            copy.addAll(headers);
            return copy;
        }

        @Override
        protected MultiValueMap<String, HttpCookie> initCookies() {
            MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
            for (String header : getHeaders().getOrEmpty(HttpHeaders.COOKIE)) {
                for (String pair : header.split(";")) {
                    int separator = pair.indexOf('=');
                    if (separator > 0) {
                        String name = pair.substring(0, separator).trim();
                        cookies.add(name, new HttpCookie(name, pair.substring(separator + 1).trim()));
                    }
                }
            }
            return cookies;
        }

        @Override
        protected SslInfo initSslInfo() {
            return null;
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
    }

    private static final class Response extends AbstractServerHttpResponse {

        Response() {
            super(DefaultDataBufferFactory.sharedInstance);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getNativeResponse() {
            return (T) this;
        }

        @Override
        protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
            return Flux.from(body).then();
        }

        @Override
        protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return Flux.from(body).flatMap(Flux::from).then();
        }

        @Override
        protected void applyStatusCode() {
        }

        @Override
        protected void applyHeaders() {
        }

        @Override
        protected void applyCookies() {
        }
    }
}
