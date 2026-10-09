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

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.github.doriangrelu.gatewayui.internal.i18n.Messages;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * Choisit la langue de l'UI pour une requête (ADR 0009), par ordre de priorité : le sélecteur de l'UI (paramètre
 * {@code lang}, mémorisé dans un cookie), le cookie, l'en-tête {@code Accept-Language}, puis la langue par défaut.
 */
public class UiLocaleResolver {

    /** Paramètre posé par le sélecteur de langue de l'UI. */
    public static final String PARAMETER = "lang";

    /** Cookie qui mémorise la langue choisie dans l'UI. */
    static final String COOKIE = "gateway-ui-lang";

    private static final Duration COOKIE_DURATION = Duration.ofDays(365);

    private final Locale defaultLocale;

    private final String cookiePath;

    /**
     * Crée le résolveur.
     *
     * @param defaultLocale langue par défaut ; l'anglais si elle n'est pas supportée
     * @param cookiePath chemin de l'UI, auquel le cookie est limité
     */
    public UiLocaleResolver(final Locale defaultLocale, final String cookiePath) {
        this.defaultLocale = Messages.supported(defaultLocale).orElse(Locale.ENGLISH);
        this.cookiePath = cookiePath;
    }

    /**
     * Langue de l'UI pour cette requête. Un choix fait dans le sélecteur est mémorisé dans un cookie de la réponse.
     *
     * @param request requête HTTP
     * @return une langue supportée
     */
    public Locale resolve(final ServerRequest request) {
        final Optional<Locale> chosen = request.queryParam(PARAMETER).map(Locale::forLanguageTag).flatMap(Messages::supported);
        chosen.ifPresent(locale -> remember(request, locale));
        return chosen.or(() -> fromCookie(request)).or(() -> fromBrowser(request)).orElse(defaultLocale);
    }

    private static Optional<Locale> fromCookie(final ServerRequest request) {
        final HttpCookie cookie = request.cookies().getFirst(COOKIE);
        return cookie == null ? Optional.empty() : Messages.supported(Locale.forLanguageTag(cookie.getValue()));
    }

    private static Optional<Locale> fromBrowser(final ServerRequest request) {
        try {
            final List<Locale.LanguageRange> ranges = request.headers().acceptLanguage();
            return Locale.filter(ranges, Messages.SUPPORTED).stream().findFirst();
        }
        catch (final IllegalArgumentException ex) {
            // En-tête Accept-Language mal formé : on passe à la langue par défaut
            return Optional.empty();
        }
    }

    private void remember(final ServerRequest request, final Locale locale) {
        request.exchange().getResponse().addCookie(ResponseCookie.from(COOKIE, locale.getLanguage())
                .path(cookiePath)
                .maxAge(COOKIE_DURATION)
                .httpOnly(true)
                .sameSite("Lax")
                .build());
    }
}
