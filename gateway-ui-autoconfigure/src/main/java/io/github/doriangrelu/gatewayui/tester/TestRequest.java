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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;

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
 */
public record TestRequest(String method, String pathAndQuery, String host, String headers, String remoteAddress) {

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
    }

    /**
     * Construit la requête à partir des paramètres du formulaire du testeur.
     *
     * @param params paramètres {@code method}, {@code path}, {@code host}, {@code headers} et {@code remoteAddress}
     * @return la requête à tester
     */
    public static TestRequest from(final MultiValueMap<String, String> params) {
        return new TestRequest(params.getFirst("method"), params.getFirst("path"), params.getFirst("host"),
                params.getFirst("headers"), params.getFirst("remoteAddress"));
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
    public String validate() {
        if (!HTTP_METHOD.matcher(method).matches()) {
            return "Méthode HTTP invalide : " + method;
        }
        try {
            return uri().getHost() == null ? "Hôte invalide : " + host : null;
        }
        catch (final IllegalArgumentException ex) {
            return "Chemin ou hôte invalide : " + ex.getMessage();
        }
    }

    HttpMethod httpMethod() {
        return HttpMethod.valueOf(method);
    }

    URI uri() {
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

    HttpHeaders httpHeaders() {
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
