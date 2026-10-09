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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Repère les valeurs qui ressemblent à un secret écrit en clair (ADR 0012).
 *
 * <p>Une valeur qui contient un placeholder ({@code ${API_TOKEN}}) n'est jamais un secret : elle sera résolue au
 * démarrage de la Gateway, à partir d'une source que le YAML exporté n'expose pas.
 */
class SecretDetector {

    private static final String PLACEHOLDER = "${";

    /** Schéma d'authentification suivi d'un jeton ({@code Bearer eyJhbGci...}) ; {@code Bearer .*} reste une regex. */
    private static final Pattern CREDENTIALS = Pattern.compile("(?i)(bearer|basic|digest|token)\\s+[A-Za-z0-9._~+/=-]{8,}");

    /** Nom d'en-tête, de paramètre ou de champ qui désigne un secret. */
    private static final Pattern SENSITIVE_NAME = Pattern.compile(
            "(?i).*(authorization|password|passwd|secret|token|api[-_.]?key|credential|private[-_.]?key).*");

    /** Valeurs de configuration qui ne peuvent pas être un secret : nombre, booléen, expression SpEL ({@code #{@bean}}). */
    private static final Pattern NOT_SECRET = Pattern.compile("(?i)[0-9.]+|true|false|#\\{.*}");

    /** Champs qui portent le nom d'un en-tête, d'un paramètre ou d'un cookie : les autres champs en sont la valeur. */
    private static final Set<String> NAME_FIELDS = Set.of("name", "header", "param");

    private SecretDetector() {
    }

    /**
     * Indique si l'URI contient un mot de passe ({@code http://user:password@host}).
     */
    static boolean uriHasPassword(final String uri) {
        if (uri.contains(PLACEHOLDER)) {
            return false;
        }
        try {
            final String userInfo = URI.create(uri.trim()).getRawUserInfo();
            return userInfo != null && userInfo.contains(":");
        }
        catch (final IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Champs d'une étape dont la valeur ressemble à un secret, dans l'ordre de l'étape.
     *
     * @param step prédicat ou filtre
     * @param regexFields champs qui portent une expression régulière, jamais considérés comme un secret
     */
    static List<String> secretFields(final EditableStep step, final Set<String> regexFields) {
        final boolean sensitiveName = NAME_FIELDS.stream()
                .map(step.values()::get)
                .anyMatch(name -> name != null && SENSITIVE_NAME.matcher(name).matches());
        return step.values().entrySet().stream()
                .filter(entry -> !regexFields.contains(entry.getKey()))
                .filter(entry -> isSecret(entry.getKey(), entry.getValue(), sensitiveName))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static boolean isSecret(final String field, final String value, final boolean sensitiveName) {
        if (isPlain(value)) {
            return false;
        }
        // Le nom d'un en-tête sensible (Authorization) n'est pas un secret : seule sa valeur l'est
        final boolean named = !NAME_FIELDS.contains(field) && (sensitiveName || SENSITIVE_NAME.matcher(field).matches());
        return named || CREDENTIALS.matcher(value.trim()).matches();
    }

    private static boolean isPlain(final String value) {
        return value == null || value.contains(PLACEHOLDER) || value.isBlank() || NOT_SECRET.matcher(value.trim()).matches();
    }
}
