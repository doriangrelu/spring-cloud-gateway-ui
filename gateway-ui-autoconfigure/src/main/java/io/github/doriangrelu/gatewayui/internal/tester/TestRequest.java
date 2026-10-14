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
package io.github.doriangrelu.gatewayui.internal.tester;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Requête saisie dans le testeur de routes. Les valeurs absentes sont remplacées par des valeurs par défaut.
 *
 * @param method méthode HTTP, {@code GET} par défaut
 * @param pathAndQuery chemin et query string, par ex. {@code /api/orders/42?expand=true}
 * @param host valeur de l'en-tête {@code Host} et de l'hôte de l'URI, {@code localhost} par défaut
 * @param headers en-têtes supplémentaires, un par ligne au format {@code Nom: valeur}
 * @param remoteAddress IP du client, utilisée par le prédicat {@code RemoteAddr}, {@code 127.0.0.1} par défaut
 * @param body corps de la requête, vide par défaut ; ignoré par la simulation, qui reproduit un corps vide (ADR 0004),
 *        et envoyé par l'appel réel pour les seules méthodes qui en portent un (ADR 0015)
 */
public record TestRequest(String method, String pathAndQuery, String host, String headers, String remoteAddress, String body) {

    /** Méthodes dont l'appel réel transmet le corps saisi. */
    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final String DEFAULT_METHOD = "GET";

    private static final String DEFAULT_HOST = "localhost";

    private static final String DEFAULT_REMOTE_ADDRESS = "127.0.0.1";

    private static final Pattern HTTP_METHOD = Pattern.compile("[A-Z]+");

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:]*:[0-9a-fA-F:.]*");

    /**
     * Applique les valeurs par défaut et rend le chemin absolu.
     */
    public TestRequest {
        method = blankTo(method, DEFAULT_METHOD).toUpperCase(Locale.ROOT);
        pathAndQuery = withLeadingSlash(blankTo(pathAndQuery, "/"));
        host = blankTo(host, DEFAULT_HOST);
        headers = headers == null ? "" : headers;
        remoteAddress = blankTo(remoteAddress, DEFAULT_REMOTE_ADDRESS);
        body = body == null ? "" : body;
    }

    /**
     * Crée une requête sans corps, comme celles de la simulation.
     *
     * @param method méthode HTTP
     * @param pathAndQuery chemin et query string
     * @param host valeur de l'en-tête {@code Host}
     * @param headers en-têtes supplémentaires, un par ligne
     * @param remoteAddress IP du client
     */
    public TestRequest(final String method, final String pathAndQuery, final String host, final String headers,
            final String remoteAddress) {
        this(method, pathAndQuery, host, headers, remoteAddress, null);
    }

    /**
     * Construit la requête à partir des paramètres du formulaire du testeur.
     *
     * @param params paramètres {@code method}, {@code path}, {@code host}, {@code headers}, {@code remoteAddress} et
     *        {@code body}
     * @return la requête à tester
     */
    public static TestRequest from(final MultiValueMap<String, String> params) {
        return new TestRequest(params.getFirst("method"), params.getFirst("path"), params.getFirst("host"),
                params.getFirst("headers"), params.getFirst("remoteAddress"), params.getFirst("body"));
    }

    /**
     * Indique si la méthode porte un corps, que l'appel réel transmet alors.
     *
     * @return {@code true} pour {@code POST}, {@code PUT}, {@code PATCH} et {@code DELETE}
     */
    public boolean carriesBody() {
        return METHODS_WITH_BODY.contains(method);
    }

    /**
     * Indique si le formulaire est vide, c'est-à-dire s'il n'y a aucune requête à tester.
     *
     * @param params paramètres du formulaire
     * @return {@code true} si aucun chemin n'a été saisi
     */
    public static boolean isEmpty(final MultiValueMap<String, String> params) {
        final String path = params.getFirst("path");
        return path == null || path.isBlank();
    }

    /**
     * Vérifie que la requête peut être testée.
     *
     * @return le message d'erreur à afficher, ou {@code null} si la requête est valide
     */
    public Message validate() {
        if (!HTTP_METHOD.matcher(method).matches()) {
            return Message.of("tester.error.method", method);
        }
        try {
            return uri().getHost() == null ? Message.of("tester.error.host", host) : null;
        }
        catch (final IllegalArgumentException ex) {
            return Message.of("tester.error.invalid", ex.getMessage());
        }
    }

    /**
     * Méthode HTTP de la requête, à n'appeler qu'après {@link #validate()}.
     *
     * @return la méthode
     */
    public HttpMethod httpMethod() {
        return HttpMethod.valueOf(method);
    }

    /**
     * URI de la requête, avec l'hôte saisi, à n'appeler qu'après {@link #validate()}.
     *
     * @return l'URI encodée
     */
    public URI uri() {
        return UriComponentsBuilder.fromUriString("http://" + host + pathAndQuery).encode().build().toUri();
    }

    /**
     * Adresse du client. Seules les IP littérales sont résolues : l'UI ne déclenche jamais de résolution DNS.
     */
    InetSocketAddress inetRemoteAddress() {
        if (IPV4.matcher(remoteAddress).matches() || IPV6.matcher(remoteAddress).matches()) {
            try {
                return new InetSocketAddress(InetAddress.getByName(remoteAddress), 0);
            }
            catch (final UnknownHostException ex) {
                // IP littérale invalide : traitée comme une adresse non résolue
            }
        }
        return InetSocketAddress.createUnresolved(remoteAddress, 0);
    }

    /**
     * En-têtes saisis, plus {@code Host} s'il n'a pas été saisi.
     *
     * @return les en-têtes de la requête
     */
    public HttpHeaders httpHeaders() {
        final HttpHeaders result = new HttpHeaders();
        headers.lines().forEach(line -> addHeaderLine(result, line));
        if (!result.containsHeader(HttpHeaders.HOST)) {
            result.set(HttpHeaders.HOST, host);
        }
        return result;
    }

    private static void addHeaderLine(final HttpHeaders target, final String line) {
        final int separator = line.indexOf(':');
        if (separator > 0) {
            target.add(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
        }
    }

    private static String withLeadingSlash(final String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String blankTo(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
