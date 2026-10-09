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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Valeurs brutes de la configuration des routes, telles qu'écrites avant la résolution des placeholders (ADR 0012).
 *
 * <p>La Gateway ne connaît que les valeurs résolues : un {@code ${ORDERS_URL}} y apparaît sous forme d'URL. Les sources
 * de propriétés de Spring conservent, elles, la valeur écrite. Seules les routes déclarées en configuration sous
 * {@code spring.cloud.gateway.server.webflux.routes} sont concernées.
 */
public class RawRouteConfiguration {

    private static final String ROUTES = "spring.cloud.gateway.server.webflux.routes";

    /** Au-delà, la liste des routes de la configuration est considérée comme terminée. */
    private static final int MAX_ROUTES = 10_000;

    private final Iterable<ConfigurationPropertySource> sources;

    private final UnaryOperator<String> resolver;

    /**
     * Crée la lecture des valeurs brutes.
     *
     * @param sources sources de propriétés, de la plus prioritaire à la moins prioritaire
     * @param resolver résolution des placeholders, pour retrouver l'identifiant effectif d'une route
     */
    public RawRouteConfiguration(final Iterable<ConfigurationPropertySource> sources, final UnaryOperator<String> resolver) {
        this.sources = sources;
        this.resolver = resolver;
    }

    /**
     * Lecture des valeurs brutes de l'environnement de l'application.
     *
     * @param environment environnement Spring
     * @return la lecture des valeurs brutes
     */
    public static RawRouteConfiguration of(final ConfigurableEnvironment environment) {
        return new RawRouteConfiguration(ConfigurationPropertySources.get(environment), environment::resolvePlaceholders);
    }

    /**
     * Préfixe de la route déclarée en configuration sous cet identifiant.
     *
     * @param id identifiant effectif de la route
     * @return le préfixe ({@code spring.cloud.gateway.server.webflux.routes[2]}), ou vide si la route n'est pas déclarée
     *     en configuration
     */
    public Optional<String> routePrefix(final String id) {
        for (int index = 0; index < MAX_ROUTES; index++) {
            final String prefix = ROUTES + "[" + index + "]";
            final Optional<String> rawId = raw(prefix + ".id");
            if (rawId.isEmpty() && raw(prefix + ".uri").isEmpty()) {
                return Optional.empty();
            }
            if (rawId.map(resolver).filter(id::equals).isPresent()) {
                return Optional.of(prefix);
            }
        }
        return Optional.empty();
    }

    /**
     * Valeur brute d'une propriété.
     *
     * @param name nom de la propriété
     * @return la valeur telle qu'écrite, ou vide si la propriété n'existe pas
     */
    public Optional<String> raw(final String name) {
        final ConfigurationPropertyName property = ConfigurationPropertyName.adapt(name, '.');
        for (final ConfigurationPropertySource source : sources) {
            final ConfigurationProperty found = source.getConfigurationProperty(property);
            if (found != null && found.getValue() != null) {
                return Optional.of(String.valueOf(found.getValue()));
            }
        }
        return Optional.empty();
    }

    /**
     * Arguments bruts d'un prédicat ou d'un filtre, en forme raccourcie ({@code Path=/a, /b}) ou développée
     * ({@code name} et {@code args}).
     *
     * @param prefix préfixe du prédicat ou du filtre ({@code ...routes[0].filters[1]})
     * @param predicate {@code true} pour un prédicat, {@code false} pour un filtre
     * @param resolvedArgs arguments résolus, pour retrouver les noms des arguments développés
     * @return les arguments bruts, indexés comme les arguments résolus
     */
    public Map<String, String> args(final String prefix, final boolean predicate, final Map<String, String> resolvedArgs) {
        final Optional<String> shortcut = raw(prefix);
        if (shortcut.isPresent()) {
            return predicate ? new PredicateDefinition(shortcut.get()).getArgs() : new FilterDefinition(shortcut.get()).getArgs();
        }
        final Map<String, String> args = new LinkedHashMap<>();
        resolvedArgs.keySet().forEach(key -> raw(prefix + ".args." + key).ifPresent(value -> args.put(key, value)));
        return args;
    }
}
