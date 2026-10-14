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
package io.github.doriangrelu.gatewayui.internal.realcall;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLException;

import io.github.doriangrelu.gatewayui.autoconfigure.GatewayUiProperties;
import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.tester.TestRequest;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Envoie la requête du testeur à la Gateway elle-même, comme le ferait un vrai client (ADR 0015).
 *
 * <p>Le client est un {@link WebClient} nu, et non le {@code WebClient.Builder} de l'application : ses personnalisations
 * (filtres OAuth2, en-têtes ajoutés) fausseraient l'appel. Reactor Netty n'y active ni la trace réseau (<i>wiretap</i>),
 * qui journaliserait les en-têtes, ni le suivi des redirections : une redirection est affichée telle quelle.
 */
public class RealCaller {

    private static final String TIMEOUT_KEY = "tester.call.error.timeout";

    /** Causes d'échec reconnues, de la plus spécifique à la plus générale. */
    private static final List<Map.Entry<Class<? extends Throwable>, String>> ERRORS = List.of(
            Map.entry(TimeoutException.class, TIMEOUT_KEY),
            Map.entry(SSLException.class, "tester.call.error.tls"),
            Map.entry(ConnectException.class, "tester.call.error.unreachable"));

    private final WebClient client = WebClient.create();

    private final LocalServerAddress address;

    private final String basePath;

    private final Duration timeout;

    private final BodyExcerptReader reader;

    private final CallRegistry registry;

    /**
     * Crée l'émetteur des appels réels.
     *
     * @param address adresse du serveur local
     * @param basePath préfixe des URL de l'UI, qui ne peuvent pas être appelées
     * @param configuration délai maximal et taille maximale de l'extrait du corps
     * @param registry registre des appels en cours, pour connaître la route réellement empruntée
     */
    public RealCaller(final LocalServerAddress address, final String basePath, final GatewayUiProperties.RealCall configuration,
            final CallRegistry registry) {
        this.address = address;
        this.basePath = basePath;
        this.timeout = configuration.timeout();
        this.reader = new BodyExcerptReader((int) Math.min(Integer.MAX_VALUE, configuration.maxBodySize().toBytes()));
        this.registry = registry;
    }

    /**
     * Envoie la requête et lit la réponse. Une requête invalide ou qui vise l'UI n'est pas envoyée.
     *
     * @param request requête saisie dans le testeur
     * @return la réponse obtenue, ou le motif de l'échec ; ne se termine jamais en erreur
     */
    public Mono<RealCallResult> call(final TestRequest request) {
        final RealCallRequest call = new RealCallRequest(request, basePath);
        final Message refusal = call.validate();
        if (refusal != null) {
            return Mono.just(RealCallResult.failure(refusal, Duration.ZERO));
        }
        return address.resolve(request.uri())
                .map(target -> send(call, target))
                .orElseGet(() -> Mono.just(RealCallResult.failure(Message.of("tester.call.error.notStarted"), Duration.ZERO)));
    }

    private Mono<RealCallResult> send(final RealCallRequest call, final URI target) {
        final long start = System.nanoTime();
        final Optional<String> callId = registry.register();
        final WebClient.RequestBodySpec spec = client.method(call.request().httpMethod()).uri(target)
                .headers(headers -> headers.addAll(call.request().httpHeaders()))
                .headers(headers -> callId.ifPresent(id -> headers.set(CallRegistry.HEADER, id)));
        final WebClient.RequestHeadersSpec<?> ready = call.body().<WebClient.RequestHeadersSpec<?>>map(spec::bodyValue).orElse(spec);
        return ready.exchangeToMono(response -> read(response, start))
                .timeout(timeout)
                .map(result -> callId.flatMap(registry::take).map(result::withRouting).orElse(result))
                .onErrorResume(error -> Mono.just(RealCallResult.failure(reasonOf(error), elapsed(start))))
                .doFinally(signal -> callId.ifPresent(registry::take));
    }

    private Mono<RealCallResult> read(final ClientResponse response, final long start) {
        final MediaType type = response.headers().contentType().orElse(null);
        final List<Map.Entry<String, String>> headers = SensitiveHeaders.masked(response.headers().asHttpHeaders());
        return reader.read(response.bodyToFlux(DataBuffer.class), type)
                .map(body -> new RealCallResult(null, response.statusCode().value(), headers, body, elapsed(start), null));
    }

    private Message reasonOf(final Throwable error) {
        final String key = ERRORS.stream()
                .filter(entry -> hasCause(error, entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("tester.call.error.failed");
        final Throwable cause = NestedExceptionUtils.getMostSpecificCause(error);
        return Message.of(key, TIMEOUT_KEY.equals(key) ? String.valueOf(timeout.toMillis()) : String.valueOf(cause.getMessage()));
    }

    private static boolean hasCause(final Throwable error, final Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null && !type.isInstance(current)) {
            current = current.getCause();
        }
        return current != null;
    }

    private static Duration elapsed(final long start) {
        return Duration.ofNanos(System.nanoTime() - start);
    }
}
