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
package io.github.doriangrelu.gatewayui.internal.tester;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import io.github.doriangrelu.gatewayui.internal.i18n.Message;
import io.github.doriangrelu.gatewayui.internal.inspect.Definition;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;

/**
 * Registre des filtres de la Gateway que le testeur sait rejouer.
 *
 * <p>Chaque effet reproduit l'algorithme de la fabrique de filtres correspondante de Spring Cloud Gateway. Seuls les
 * filtres sans effet de bord (chemin, en-têtes, paramètres) figurent ici : un filtre absent du registre n'est jamais
 * exécuté.
 */
class FilterEffects {

    private static final Set<String> PATH_FILTERS = Set.of("StripPrefix", "PrefixPath", "RewritePath", "SetPath");

    private static final Map<String, FilterEffect> EFFECTS = Map.of(
            "StripPrefix", FilterEffects::stripPrefix,
            "PrefixPath", FilterEffects::prefixPath,
            "RewritePath", FilterEffects::rewritePath,
            "SetPath", FilterEffects::setPath,
            "AddRequestHeader", FilterEffects::addRequestHeader,
            "SetRequestHeader", FilterEffects::setRequestHeader,
            "RemoveRequestHeader", FilterEffects::removeRequestHeader,
            "AddRequestParameter", FilterEffects::addRequestParameter,
            "SetRequestHostHeader", FilterEffects::setRequestHostHeader,
            "PreserveHostHeader", FilterEffects::preserveHostHeader);

    private FilterEffects() {
    }

    /**
     * Effet d'un filtre, s'il est connu.
     *
     * @param filterName nom de la fabrique du filtre
     * @return l'effet, ou vide si le filtre ne peut pas être simulé
     */
    static Optional<FilterEffect> find(final String filterName) {
        return Optional.ofNullable(EFFECTS.get(filterName));
    }

    /**
     * Indique si le filtre modifie le chemin : son effet se décrit alors par le chemin avant et après.
     *
     * @param filterName nom de la fabrique du filtre
     * @return {@code true} pour un filtre de chemin
     */
    static boolean isPathFilter(final String filterName) {
        return PATH_FILTERS.contains(filterName);
    }

    /**
     * Même algorithme que {@code StripPrefixGatewayFilterFactory} : retire les {@code parts} premiers segments en
     * conservant le slash final.
     *
     * @param path chemin brut
     * @param parts nombre de segments à retirer
     * @return le nouveau chemin, toujours absolu
     */
    static String stripPrefix(final String path, final int parts) {
        final String[] segments = StringUtils.tokenizeToStringArray(path, "/");
        final StringBuilder newPath = new StringBuilder("/");
        for (int i = parts; i < segments.length; i++) {
            if (newPath.length() > 1) {
                newPath.append('/');
            }
            newPath.append(segments[i]);
        }
        if (newPath.length() > 1 && path.endsWith("/")) {
            newPath.append('/');
        }
        return newPath.toString();
    }

    private static Message stripPrefix(final Definition definition, final SimulationState state) {
        final String parts = definition.arg("parts", 0);
        state.path(stripPrefix(state.path(), parts == null ? 1 : Integer.parseInt(parts.trim())));
        return null;
    }

    private static Message prefixPath(final Definition definition, final SimulationState state) {
        state.path(expandPath(definition.arg("prefix", 0), state) + state.path());
        return null;
    }

    private static Message rewritePath(final Definition definition, final SimulationState state) {
        // Le YAML impose d'écrire $\{groupe} : la Gateway le ramène à ${groupe}
        final String replacement = definition.arg("replacement", 1).replace("$\\", "$");
        state.path(Pattern.compile(definition.arg("regexp", 0)).matcher(state.path()).replaceAll(replacement));
        return null;
    }

    private static Message setPath(final Definition definition, final SimulationState state) {
        state.path(expandPath(definition.arg("template", 0), state));
        return null;
    }

    private static Message addRequestHeader(final Definition definition, final SimulationState state) {
        final String name = definition.arg("name", 0);
        final String value = state.expand(definition.arg("value", 1));
        state.headers().add(name, value);
        return Message.of("sim.headerAdded", name, value);
    }

    private static Message setRequestHeader(final Definition definition, final SimulationState state) {
        final String name = definition.arg("name", 0);
        final String value = state.expand(definition.arg("value", 1));
        state.headers().set(name, value);
        return Message.of("sim.headerSet", name, value);
    }

    private static Message removeRequestHeader(final Definition definition, final SimulationState state) {
        final String name = definition.arg("name", 0);
        state.headers().remove(name);
        return Message.of("sim.headerRemoved", name);
    }

    private static Message addRequestParameter(final Definition definition, final SimulationState state) {
        final String name = definition.arg("name", 0);
        final String value = state.expand(definition.arg("value", 1));
        final String parameter = encode(name) + "=" + encode(value);
        state.query(StringUtils.hasText(state.query()) ? state.query() + "&" + parameter : parameter);
        return Message.of("sim.paramAdded", name, value);
    }

    private static Message setRequestHostHeader(final Definition definition, final SimulationState state) {
        final String host = definition.arg("host", 0);
        state.headers().set(HttpHeaders.HOST, host);
        state.preserveHost(true);
        return Message.of("sim.hostForced", host);
    }

    private static Message preserveHostHeader(final Definition definition, final SimulationState state) {
        state.preserveHost(true);
        return Message.of("sim.hostPreserved");
    }

    private static String expandPath(final String template, final SimulationState state) {
        return new UriTemplate(template).expand(state.uriVariables()).getRawPath();
    }

    private static String encode(final String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }
}
