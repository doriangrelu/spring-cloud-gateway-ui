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

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.core.env.Environment;

/**
 * Adresse du serveur qui héberge l'UI, et donc la Gateway : l'appel réel lui est envoyé en boucle locale (ADR 0015).
 *
 * <p>Le port est lu au moment de l'appel dans {@code local.server.port}, que Spring Boot renseigne au démarrage du
 * serveur web : c'est le port effectif, y compris avec {@code server.port=0}.
 */
public class LocalServerAddress {

    private static final String LOOPBACK = "127.0.0.1";

    private static final List<String> SSL_SOURCES = List.of("server.ssl.key-store", "server.ssl.bundle", "server.ssl.certificate");

    private final Environment environment;

    /**
     * Crée l'adresse du serveur local.
     *
     * @param environment environnement de l'application, pour le port effectif et la configuration TLS
     */
    public LocalServerAddress(final Environment environment) {
        this.environment = environment;
    }

    /**
     * URL locale qui correspond à l'URI saisie : même chemin et même query string, sur le serveur local.
     *
     * @param requested URI de la requête saisie
     * @return l'URL à appeler, ou vide si le serveur web n'est pas démarré
     */
    public Optional<URI> resolve(final URI requested) {
        final Integer port = environment.getProperty("local.server.port", Integer.class);
        if (port == null) {
            return Optional.empty();
        }
        final String query = requested.getRawQuery() == null ? "" : "?" + requested.getRawQuery();
        return Optional.of(URI.create(scheme() + "://" + LOOPBACK + ":" + port + requested.getRawPath() + query));
    }

    /** Spring Boot n'active TLS que si un certificat est configuré et que {@code server.ssl.enabled} ne l'interdit pas. */
    private String scheme() {
        final boolean configured = SSL_SOURCES.stream().anyMatch(environment::containsProperty);
        final boolean enabled = environment.getProperty("server.ssl.enabled", Boolean.class, Boolean.TRUE);
        return configured && enabled ? "https" : "http";
    }
}
