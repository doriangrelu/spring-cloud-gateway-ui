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

import java.util.Map;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

/**
 * Requête en cours de transformation pendant la simulation des filtres d'une route.
 *
 * <p>Seuls le chemin, la query string, les en-têtes et la conservation du {@code Host} évoluent ; l'échange d'origine
 * sert à résoudre les variables extraites par les prédicats.
 */
class SimulationState {

    private final ServerWebExchange exchange;

    private final Map<String, String> uriVariables;

    private final HttpHeaders headers = new HttpHeaders();

    private String path;

    private String query;

    private boolean preserveHost;

    /**
     * Initialise l'état à partir de la requête sur laquelle la route a été retenue.
     *
     * @param exchange échange simulé, après évaluation des prédicats
     */
    SimulationState(final ServerWebExchange exchange) {
        this.exchange = exchange;
        this.uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange);
        this.headers.addAll(exchange.getRequest().getHeaders());
        this.path = exchange.getRequest().getURI().getRawPath();
        this.query = exchange.getRequest().getURI().getRawQuery();
    }

    /**
     * Remplace les variables {@code {nom}} d'un modèle par celles extraites par les prédicats, comme les filtres de la
     * Gateway.
     *
     * @param template modèle de valeur
     * @return la valeur résolue
     */
    String expand(final String template) {
        return ServerWebExchangeUtils.expand(exchange, template);
    }

    Map<String, String> uriVariables() {
        return uriVariables;
    }

    HttpHeaders headers() {
        return headers;
    }

    String path() {
        return path;
    }

    void path(final String newPath) {
        this.path = newPath;
    }

    String query() {
        return query;
    }

    void query(final String newQuery) {
        this.query = newQuery;
    }

    boolean preserveHost() {
        return preserveHost;
    }

    void preserveHost(final boolean preserve) {
        this.preserveHost = preserve;
    }
}
