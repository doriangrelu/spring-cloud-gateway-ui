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
package io.github.doriangrelu.gatewayui.internal.i18n;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Messages de l'UI dans les langues supportées.
 *
 * <p>Les fichiers {@code messages_xx.properties} sont lus par cette classe seule, sans passer par le
 * {@code MessageSource} de Spring : l'internationalisation de l'application hôte n'est jamais affectée (ADR 0009).
 */
public class Messages {

    /** Langues supportées, dans l'ordre d'affichage du sélecteur. */
    public static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, Locale.FRENCH);

    private static final String BUNDLE = "io.github.doriangrelu.gatewayui.internal.i18n.messages";

    private final Map<Locale, ResourceBundle> bundles;

    /**
     * Charge les messages de toutes les langues supportées.
     */
    public Messages() {
        this.bundles = SUPPORTED.stream().collect(Collectors.toUnmodifiableMap(Function.identity(), Messages::load));
    }

    /**
     * Retient la langue supportée qui correspond à une langue demandée.
     *
     * @param requested langue demandée (par ex. {@code fr-CA} donne {@code fr}) ; peut être {@code null}
     * @return la langue supportée, ou vide si la langue n'est pas supportée
     */
    public static Optional<Locale> supported(final Locale requested) {
        if (requested == null) {
            return Optional.empty();
        }
        return SUPPORTED.stream().filter(locale -> locale.getLanguage().equals(requested.getLanguage())).findFirst();
    }

    /**
     * Texte d'un message dans une langue.
     *
     * @param locale langue supportée
     * @param key clé du message
     * @param args arguments du message
     * @return le texte, ou la clé entre crochets si elle est inconnue
     */
    public String text(final Locale locale, final String key, final Object... args) {
        final ResourceBundle bundle = bundles.get(supported(locale).orElse(Locale.ENGLISH));
        if (!bundle.containsKey(key)) {
            return "[" + key + "]";
        }
        final String pattern = bundle.getString(key);
        return args.length == 0 ? pattern : new MessageFormat(pattern, locale).format(args);
    }

    /**
     * Indique si un message existe dans une langue : le catalogue est rédigé progressivement (ADR 0013).
     *
     * @param locale langue supportée
     * @param key clé du message
     * @return {@code true} si le message existe
     */
    public boolean contains(final Locale locale, final String key) {
        return bundles.get(supported(locale).orElse(Locale.ENGLISH)).containsKey(key);
    }

    /**
     * Texte d'un message produit par l'implémentation.
     *
     * @param locale langue supportée
     * @param message message à traduire
     * @return le texte
     */
    public String text(final Locale locale, final Message message) {
        return text(locale, message.key(), message.args().toArray());
    }

    /**
     * Clés d'une langue, pour vérifier que toutes les langues sont complètes.
     *
     * @param locale langue supportée
     * @return les clés
     */
    public Set<String> keys(final Locale locale) {
        return bundles.get(locale).keySet();
    }

    private static ResourceBundle load(final Locale locale) {
        // Pas de repli sur la langue du système : chaque langue doit être complète
        return ResourceBundle.getBundle(BUNDLE, locale, Messages.class.getClassLoader(),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }
}
