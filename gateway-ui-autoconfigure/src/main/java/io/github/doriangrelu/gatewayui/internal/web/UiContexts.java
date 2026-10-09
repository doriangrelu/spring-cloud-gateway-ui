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

import java.util.Locale;

import io.github.doriangrelu.gatewayui.internal.i18n.Messages;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * Construit le {@link UiContext} de chaque requête : chemin de l'UI, onglet, langue et thème choisis par l'utilisateur.
 */
public class UiContexts {

    private final String basePath;

    private final Messages messages;

    private final UiLocaleResolver localeResolver;

    /**
     * Crée la fabrique de contextes.
     *
     * @param basePath préfixe des URL de l'UI
     * @param defaultLocale langue par défaut de l'UI
     */
    public UiContexts(final String basePath, final Locale defaultLocale) {
        this.basePath = basePath;
        this.messages = new Messages();
        this.localeResolver = new UiLocaleResolver(defaultLocale, basePath);
    }

    /**
     * Contexte d'une page.
     *
     * @param request requête HTTP ; un changement de langue ou de thème demandé par un sélecteur y est mémorisé
     * @param page onglet actif
     * @return le contexte de rendu
     */
    public UiContext create(final ServerRequest request, final String page) {
        return new UiContext(basePath, page, localeResolver.resolve(request), UiTheme.resolve(request, basePath), messages,
                request.uri());
    }

    /**
     * Préfixe des URL de l'UI.
     *
     * @return le chemin de base
     */
    public String basePath() {
        return basePath;
    }
}
