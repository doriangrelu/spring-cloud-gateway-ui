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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Texte à traduire au rendu : une clé de message et ses arguments.
 *
 * <p>Permet aux paquets {@code inspect} et {@code tester} de produire des textes affichables sans connaître la langue de
 * l'utilisateur.
 *
 * @param key clé dans les fichiers de messages
 * @param args arguments insérés dans le message ({@code {0}}, {@code {1}}...)
 */
public record Message(String key, List<Object> args) {

    /**
     * Copie les arguments, qui peuvent être {@code null} (par ex. le message d'une exception).
     */
    public Message {
        args = Collections.unmodifiableList(new ArrayList<>(args));
    }

    /**
     * Crée un message.
     *
     * @param key clé dans les fichiers de messages
     * @param args arguments du message
     * @return le message
     */
    public static Message of(final String key, final Object... args) {
        return new Message(key, Arrays.asList(args));
    }
}
