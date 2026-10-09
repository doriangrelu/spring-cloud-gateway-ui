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
package io.github.doriangrelu.gatewayui.internal.editor;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;

/**
 * Conseil sur une route éditée : erreur qui empêche la Gateway de l'accepter, ou avertissement sur un risque.
 *
 * @param level gravité du conseil
 * @param target élément concerné, pour que l'UI place le conseil : {@code id}, {@code uri}, {@code predicates},
 *        {@code predicates[0]} (la fabrique), {@code predicates[0].regexp}, {@code filters[1].value}...
 * @param message texte du conseil, à traduire
 */
public record Advice(Level level, String target, Message message) {

    /**
     * Erreur sur un élément de la route.
     *
     * @param target élément concerné
     * @param key clé du message
     * @param args arguments du message
     * @return le conseil
     */
    public static Advice error(final String target, final String key, final Object... args) {
        return new Advice(Level.ERROR, target, Message.of(key, args));
    }

    /**
     * Avertissement sur un élément de la route.
     *
     * @param target élément concerné
     * @param key clé du message
     * @param args arguments du message
     * @return le conseil
     */
    public static Advice warning(final String target, final String key, final Object... args) {
        return new Advice(Level.WARNING, target, Message.of(key, args));
    }

    /**
     * Gravité d'un conseil.
     */
    public enum Level {

        /** La Gateway refuserait la route, ou la route ne peut pas fonctionner. */
        ERROR,

        /** La route est acceptée, mais présente un risque. */
        WARNING
    }
}
