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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;

/**
 * Masque les en-têtes sensibles dans le résultat d'un appel réel (FR-011, ADR 0015).
 *
 * <p>Seul le résultat de l'appel réel est concerné : la simulation et le formulaire du testeur réaffichent la saisie
 * comme avant. La liste est propre aux en-têtes HTTP ; {@code SecretDetector}, qui joue un rôle voisin pour l'éditeur,
 * vit dans un paquet dont celui-ci ne peut pas dépendre.
 */
class SensitiveHeaders {

    /** Valeur affichée à la place d'un en-tête sensible. */
    static final String MASK = "••••";

    private static final Set<String> NAMES = Set.of("authorization", "proxy-authorization", "cookie", "set-cookie");

    private static final Pattern SENSITIVE_PART = Pattern.compile("(?i).*(token|secret|password|api[-_.]?key|credential).*");

    private SensitiveHeaders() {
    }

    static boolean isSensitive(final String name) {
        return NAMES.contains(name.toLowerCase(Locale.ROOT)) || SENSITIVE_PART.matcher(name).matches();
    }

    /** Une ligne par valeur, dans l'ordre reçu, valeurs sensibles masquées. */
    static List<Map.Entry<String, String>> masked(final HttpHeaders headers) {
        final List<Map.Entry<String, String>> lines = new ArrayList<>();
        headers.forEach((name, values) -> values.forEach(value -> lines.add(Map.entry(name, isSensitive(name) ? MASK : value))));
        return List.copyOf(lines);
    }
}
