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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Données communes à toutes les pages : construction des URL de l'UI et onglet actif de la navigation.
 *
 * @param basePath préfixe des URL de l'UI
 * @param page onglet actif ({@code routes}, {@code services}, {@code filters} ou {@code tester})
 */
public record UiContext(String basePath, String page) {

    /**
     * URL d'une page de l'UI.
     *
     * @param path chemin relatif à l'UI, commençant par {@code /}
     * @return l'URL absolue
     */
    public String url(final String path) {
        return basePath + path;
    }

    /**
     * URL du détail d'une route, l'id étant encodé.
     *
     * @param routeId identifiant de la route
     * @return l'URL de la page de détail
     */
    public String routeUrl(final String routeId) {
        return basePath + "/routes/" + URLEncoder.encode(routeId, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * URL d'une ressource statique de l'UI.
     *
     * @param name nom du fichier
     * @return l'URL de la ressource
     */
    public String asset(final String name) {
        return basePath + "/assets/" + name;
    }

    /**
     * Classes CSS d'un lien de navigation.
     *
     * @param target onglet du lien
     * @return les classes, avec {@code active} pour l'onglet courant
     */
    public String navClass(final String target) {
        return target.equals(page) ? "nav-link active" : "nav-link";
    }

    /**
     * Même contexte pour un autre onglet.
     *
     * @param newPage onglet actif
     * @return le nouveau contexte
     */
    UiContext page(final String newPage) {
        return new UiContext(basePath, newPage);
    }
}
