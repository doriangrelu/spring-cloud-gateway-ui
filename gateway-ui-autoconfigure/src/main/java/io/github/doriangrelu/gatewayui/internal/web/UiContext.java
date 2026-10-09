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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.i18n.Messages;
import io.github.doriangrelu.gatewayui.internal.inspect.FilterStep;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Données communes à toutes les pages : URL de l'UI, onglet actif, langue et messages traduits.
 *
 * @param basePath préfixe des URL de l'UI
 * @param page onglet actif ({@code routes}, {@code services}, {@code filters} ou {@code tester})
 * @param locale langue de la page
 * @param messages messages de l'UI
 * @param requestUri URI de la page affichée, pour construire les liens du sélecteur de langue
 */
public record UiContext(String basePath, String page, Locale locale, Messages messages, URI requestUri) {

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
     * Message traduit dans la langue de la page.
     *
     * @param key clé du message
     * @param args arguments du message
     * @return le texte
     */
    public String message(final String key, final Object... args) {
        return messages.text(locale, key, args);
    }

    /**
     * Message produit par l'implémentation, traduit dans la langue de la page.
     *
     * @param message message à traduire
     * @return le texte
     */
    public String message(final Message message) {
        return messages.text(locale, message);
    }

    /**
     * Code de la langue de la page, pour l'attribut {@code lang} du document.
     *
     * @return le code ISO de la langue ({@code en}, {@code fr})
     */
    public String lang() {
        return locale.getLanguage();
    }

    /**
     * Langues proposées par le sélecteur.
     *
     * @return les langues supportées
     */
    public List<Locale> languages() {
        return Messages.SUPPORTED;
    }

    /**
     * URL de la page courante dans une autre langue : mêmes paramètres, langue remplacée.
     *
     * @param language langue cible
     * @return l'URL relative de la page
     */
    public String languageUrl(final Locale language) {
        return UriComponentsBuilder.fromUri(requestUri)
                .scheme(null).host(null).port(-1)
                .replaceQueryParam(UiLocaleResolver.PARAMETER, language.getLanguage())
                // Les paramètres de la page sont déjà encodés (chemins du testeur) : ne pas les ré-encoder
                .build(true)
                .toUriString();
    }

    /**
     * Ordre affiché d'un filtre.
     *
     * @param step filtre
     * @return l'ordre, ou la mention « non ordonné »
     */
    public String orderText(final FilterStep step) {
        return step.ordered() ? step.orderText() : message("order.unordered");
    }

    /**
     * Infobulle de l'ordre d'un filtre : un filtre sans ordre explicite est souvent une erreur de configuration.
     *
     * @param step filtre
     * @return le texte de l'infobulle
     */
    public String orderHint(final FilterStep step) {
        return message(step.ordered() ? "order.title" : "order.unorderedHint");
    }

    /**
     * Description d'un filtre : ses arguments ou son type, ou la classe qui déclare une lambda.
     *
     * @param step filtre
     * @return la description
     */
    public String filterDescription(final FilterStep step) {
        return step.lambda() ? message("filter.lambdaIn", step.description()) : step.description();
    }
}
