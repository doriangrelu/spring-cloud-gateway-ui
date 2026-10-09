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

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * Thème de l'UI, choisi depuis la barre de navigation et mémorisé dans un cookie.
 *
 * <p>Le thème est appliqué côté serveur (attribut {@code data-theme} du document) : la page s'affiche directement dans le
 * bon thème, sans JavaScript ni changement visible au chargement.
 */
public enum UiTheme {

    /** Thème du système ({@code prefers-color-scheme}). */
    AUTO,

    /** Thème clair, quel que soit le système. */
    LIGHT,

    /** Thème sombre, quel que soit le système. */
    DARK;

    /** Paramètre posé par le sélecteur de thème de l'UI. */
    public static final String PARAMETER = "theme";

    /** Cookie qui mémorise le thème choisi dans l'UI. */
    static final String COOKIE = "gateway-ui-theme";

    /**
     * Thème de l'UI pour cette requête : celui choisi dans le sélecteur (alors mémorisé), sinon celui mémorisé,
     * sinon le thème du système.
     *
     * @param request requête HTTP
     * @param cookiePath chemin de l'UI, auquel le cookie est limité
     * @return le thème
     */
    static UiTheme resolve(final ServerRequest request, final String cookiePath) {
        final Optional<UiTheme> chosen = request.queryParam(PARAMETER).flatMap(UiTheme::parse);
        chosen.ifPresent(theme -> PreferenceCookies.remember(request, COOKIE, theme.code(), cookiePath));
        return chosen.or(() -> PreferenceCookies.read(request, COOKIE).flatMap(UiTheme::parse)).orElse(AUTO);
    }

    /**
     * Code du thème, utilisé dans les URL, le cookie et les clés de message.
     *
     * @return {@code auto}, {@code light} ou {@code dark}
     */
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Valeur de l'attribut {@code data-theme} du document.
     *
     * @return le code du thème, ou {@code null} pour suivre le système (attribut absent)
     */
    public String attribute() {
        return this == AUTO ? null : code();
    }

    private static Optional<UiTheme> parse(final String code) {
        return Arrays.stream(values()).filter(theme -> theme.code().equals(code)).findFirst();
    }
}
