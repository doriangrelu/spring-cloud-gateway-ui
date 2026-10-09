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
package io.github.doriangrelu.gatewayui.inspect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.support.NameUtils;

/**
 * Prédicat ou filtre tel que déclaré dans la configuration, par exemple {@code Path=/api/**} ou {@code StripPrefix=1}.
 *
 * <p>Les arguments déclarés en forme raccourcie ({@code RewritePath=/a, /b}) sont indexés par la Gateway sous des
 * clés générées ({@code _genkey_0}, {@code _genkey_1}...) ; {@link #arg(String, int)} les retrouve par nom ou par
 * position.
 *
 * @param name nom de la fabrique, par exemple {@code Path} ou {@code StripPrefix}
 * @param args arguments dans leur ordre de déclaration
 */
public record Definition(String name, Map<String, String> args) {

    /**
     * Copie les arguments en conservant leur ordre.
     */
    public Definition {
        args = args == null ? Map.of() : new LinkedHashMap<>(args);
    }

    /**
     * Crée la vue d'un prédicat déclaré.
     *
     * @param definition déclaration du prédicat
     * @return la vue du prédicat
     */
    public static Definition of(final PredicateDefinition definition) {
        return new Definition(definition.getName(), definition.getArgs());
    }

    /**
     * Crée la vue d'un filtre déclaré.
     *
     * @param definition déclaration du filtre
     * @return la vue du filtre
     */
    public static Definition of(final FilterDefinition definition) {
        return new Definition(definition.getName(), definition.getArgs());
    }

    /**
     * Résume des prédicats sous la forme {@code Path=/api/** && Method=GET}.
     *
     * @param predicates prédicats d'une route
     * @return le résumé, vide s'il n'y a aucun prédicat
     */
    public static String summary(final List<Definition> predicates) {
        return predicates.stream()
                .map(definition -> definition.name() + "=" + definition.argsText())
                .collect(Collectors.joining(" && "));
    }

    /**
     * Retourne un argument par son nom (forme développée) ou par sa position (forme raccourcie).
     *
     * @param key nom de l'argument dans la forme développée
     * @param index position de l'argument dans la forme raccourcie
     * @return la valeur, ou {@code null} si l'argument est absent
     */
    public String arg(final String key, final int index) {
        if (args.containsKey(key)) {
            return args.get(key);
        }
        final String generated = args.get(NameUtils.generateName(index));
        return generated != null ? generated : argAtPosition(index);
    }

    /**
     * Rend les arguments comme dans la forme raccourcie du YAML, ou sous forme {@code clé: valeur} s'ils sont nommés.
     *
     * @return les arguments mis en forme
     */
    public String argsText() {
        if (isShortcut()) {
            return String.join(", ", args.values());
        }
        return args.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private boolean isShortcut() {
        return args.keySet().stream().allMatch(key -> key.startsWith(NameUtils.GENERATED_NAME_PREFIX));
    }

    private String argAtPosition(final int index) {
        final List<String> values = new ArrayList<>(args.values());
        return index < values.size() ? values.get(index) : null;
    }
}
