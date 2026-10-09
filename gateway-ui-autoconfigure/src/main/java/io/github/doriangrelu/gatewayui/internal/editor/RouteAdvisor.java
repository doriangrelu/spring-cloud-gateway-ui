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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;

/**
 * Conseils sur une route éditée, dans le contexte des autres routes de l'espace de travail (ADR 0011, 0012).
 *
 * <p>Les erreurs signalent ce que la Gateway refuserait au démarrage ou ce qui empêche la route de fonctionner :
 * identifiant absent ou en double, URI invalide, fabrique inconnue, argument obligatoire vide, expression régulière
 * invalide. Les avertissements signalent un risque : route sans prédicat, {@code Path=/**}, secret écrit en clair.
 *
 * <p>Les placeholders sont résolus avec l'environnement de l'application, comme le ferait la Gateway au démarrage.
 * Un placeholder qui ne se résout pas ici est accepté : sa valeur viendra de la configuration de déploiement, que
 * l'UI ne connaît pas.
 */
public class RouteAdvisor {

    private static final String PLACEHOLDER = "${";

    private static final String ID = "id";

    private static final String URI_TARGET = "uri";

    private static final String PREDICATES = "predicates";

    private static final String FILTERS = "filters";

    private static final String CATCH_ALL = "/**";

    /** Arguments compilés en {@link Pattern} par les fabriques de prédicats de la Gateway. */
    private static final Map<String, Set<String>> PREDICATE_REGEX_FIELDS = Map.of(
            "Cookie", Set.of("regexp"),
            "Header", Set.of("regexp"),
            "Query", Set.of("regexp"));

    /** Arguments compilés en {@link Pattern} par les fabriques de filtres de la Gateway. */
    private static final Map<String, Set<String>> FILTER_REGEX_FIELDS = Map.of(
            "RewritePath", Set.of("regexp"),
            "RewriteResponseHeader", Set.of("regexp"),
            "RewriteLocationResponseHeader", Set.of("protocols"));

    /** Arguments facultatifs des prédicats standard : la Gateway leur donne une valeur par défaut. */
    private static final Map<String, Set<String>> PREDICATE_OPTIONAL_FIELDS = Map.of(
            "Path", Set.of("matchTrailingSlash"),
            "Header", Set.of("regexp"),
            "Query", Set.of("regexp", "predicate"));

    /** Arguments facultatifs des filtres standard : la Gateway leur donne une valeur par défaut. */
    private static final Map<String, Set<String>> FILTER_OPTIONAL_FIELDS = Map.of(
            "AddResponseHeader", Set.of("override"),
            "CircuitBreaker", Set.of("name"),
            "DedupeResponseHeader", Set.of("strategy"),
            "FallbackHeaders", Set.of("name"),
            "RedirectTo", Set.of("includeRequestParams"),
            "RemoveJsonAttributesResponseBody", Set.of("deleteRecursively"),
            "Retry", Set.of("retries", "statuses", "methods", "backoff.firstBackoff", "backoff.maxBackoff", "backoff.factor",
                    "backoff.basedOnPreviousValue", "jitter.randomFactor", "timeout"),
            "RewriteLocationResponseHeader", Set.of("stripVersion", "locationHeaderName", "hostValue", "protocols"),
            "StripPrefix", Set.of("parts"),
            "TokenRelay", Set.of("name"));

    private final UnaryOperator<String> resolver;

    private final StepKind predicates;

    private final StepKind filters;

    /**
     * Crée le conseiller.
     *
     * @param catalog fabriques de la Gateway
     * @param resolver résolution des placeholders, comme lors de la lecture de la configuration
     */
    public RouteAdvisor(final FactoryCatalog catalog, final UnaryOperator<String> resolver) {
        this.resolver = resolver;
        this.predicates = new StepKind(PREDICATES, catalog::knowsPredicate, catalog::predicate, PREDICATE_REGEX_FIELDS,
                PREDICATE_OPTIONAL_FIELDS);
        this.filters = new StepKind(FILTERS, catalog::knowsFilter, catalog::filter, FILTER_REGEX_FIELDS, FILTER_OPTIONAL_FIELDS);
    }

    /**
     * Conseils sur une route éditée.
     *
     * @param route route éditée
     * @param others autres routes de l'espace de travail, sans la route éditée
     * @return les conseils, dans l'ordre des éléments de la route
     */
    public List<Advice> advise(final EditableRoute route, final List<EditableRoute> others) {
        final List<Advice> advice = new ArrayList<>(identifier(route.id(), others));
        advice.addAll(uri(route.uri()));
        advice.addAll(routePredicates(route.predicates()));
        advice.addAll(steps(route.filters(), filters));
        return List.copyOf(advice);
    }

    private static List<Advice> identifier(final String id, final List<EditableRoute> others) {
        if (id.isBlank()) {
            return List.of(Advice.error(ID, "editor.advice.id.empty"));
        }
        final boolean duplicate = others.stream().anyMatch(other -> other.id().trim().equals(id.trim()));
        return duplicate ? List.of(Advice.error(ID, "editor.advice.id.duplicate", id.trim())) : List.of();
    }

    private List<Advice> uri(final String uri) {
        final List<Advice> advice = new ArrayList<>();
        invalidUri(uri).ifPresent(advice::add);
        if (SecretDetector.uriHasPassword(uri)) {
            advice.add(Advice.warning(URI_TARGET, "editor.advice.secret", URI_TARGET));
        }
        return advice;
    }

    private Optional<Advice> invalidUri(final String uri) {
        if (uri.isBlank()) {
            return Optional.of(Advice.error(URI_TARGET, "editor.advice.uri.empty"));
        }
        final String resolved = resolve(uri).trim();
        return resolved.contains(PLACEHOLDER) ? Optional.empty() : parse(resolved);
    }

    /**
     * Même analyse que la Gateway ({@code URI.create} dans {@code RouteDefinition}). Une URI opaque est refusée :
     * {@code orders.test:8080} serait lue comme le schéma {@code orders.test}, sans hôte.
     */
    private static Optional<Advice> parse(final String uri) {
        try {
            final URI parsed = new URI(uri);
            final boolean absolute = parsed.isAbsolute() && !parsed.isOpaque();
            return absolute ? Optional.empty() : Optional.of(Advice.error(URI_TARGET, "editor.advice.uri.relative", uri));
        }
        catch (final URISyntaxException ex) {
            return Optional.of(Advice.error(URI_TARGET, "editor.advice.uri.invalid", uri, ex.getReason()));
        }
    }

    private List<Advice> routePredicates(final List<EditableStep> steps) {
        if (steps.isEmpty()) {
            // Sans prédicat, la Gateway combine un prédicat toujours vrai (RouteDefinitionRouteLocator.combinePredicates)
            return List.of(Advice.warning(PREDICATES, "editor.advice.predicates.none"));
        }
        final List<Advice> advice = new ArrayList<>(steps(steps, predicates));
        IntStream.range(0, steps.size())
                .filter(index -> isCatchAll(steps.get(index)))
                .mapToObj(index -> Advice.warning(PREDICATES + "[" + index + "].patterns", "editor.advice.predicates.catchAll"))
                .forEach(advice::add);
        return advice;
    }

    private static boolean isCatchAll(final EditableStep step) {
        final String patterns = step.values().get("patterns");
        return "Path".equals(step.name()) && patterns != null
                && Arrays.stream(patterns.split(",")).map(String::trim).anyMatch(CATCH_ALL::equals);
    }

    private List<Advice> steps(final List<EditableStep> steps, final StepKind kind) {
        return IntStream.range(0, steps.size())
                .mapToObj(index -> step(steps.get(index), kind.target() + "[" + index + "]", kind))
                .flatMap(List::stream)
                .toList();
    }

    private List<Advice> step(final EditableStep step, final String target, final StepKind kind) {
        final String name = step.name() == null ? "" : step.name().trim();
        final List<Advice> advice = new ArrayList<>(arguments(step, name, target, kind));
        SecretDetector.secretFields(step, kind.regexFields().getOrDefault(name, Set.of())).stream()
                .map(field -> Advice.warning(target + "." + field, "editor.advice.secret", field))
                .forEach(advice::add);
        return advice;
    }

    private List<Advice> arguments(final EditableStep step, final String name, final String target, final StepKind kind) {
        if (name.isEmpty()) {
            return List.of(Advice.error(target, "editor.advice.factory.missing"));
        }
        if (!kind.known().test(name)) {
            return List.of(Advice.error(target, "editor.advice.factory.unknown", name));
        }
        final List<Advice> advice = new ArrayList<>(missingArguments(step, name, target, kind));
        advice.addAll(invalidRegexes(step, name, target, kind));
        return advice;
    }

    private static List<Advice> missingArguments(final EditableStep step, final String name, final String target,
            final StepKind kind) {
        final Set<String> optional = kind.optionalFields().getOrDefault(name, Set.of());
        return kind.descriptors().apply(name).fields().stream()
                .filter(field -> !optional.contains(field))
                .filter(field -> isBlank(step.values().get(field)))
                .map(field -> Advice.error(target + "." + field, "editor.advice.argument.empty", field, name))
                .toList();
    }

    private List<Advice> invalidRegexes(final EditableStep step, final String name, final String target, final StepKind kind) {
        final Set<String> regexFields = kind.regexFields().getOrDefault(name, Set.of());
        return step.values().entrySet().stream()
                .filter(entry -> regexFields.contains(entry.getKey()) && !isBlank(entry.getValue()))
                .flatMap(entry -> invalidRegex(target + "." + entry.getKey(), entry.getKey(), entry.getValue()).stream())
                .toList();
    }

    private Optional<Advice> invalidRegex(final String target, final String field, final String regex) {
        final String resolved = resolve(regex);
        if (resolved.contains(PLACEHOLDER)) {
            return Optional.empty();
        }
        try {
            Pattern.compile(resolved);
            return Optional.empty();
        }
        catch (final PatternSyntaxException ex) {
            return Optional.of(Advice.error(target, "editor.advice.regex.invalid", field, ex.getDescription()));
        }
    }

    private String resolve(final String value) {
        try {
            return resolver.apply(value);
        }
        catch (final IllegalArgumentException ex) {
            // Placeholder circulaire : traité comme un placeholder qui ne se résout pas ici
            return value;
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    /**
     * Prédicats ou filtres : fabriques de la Gateway et arguments particuliers de chacune.
     *
     * @param target préfixe de la cible des conseils
     * @param known indique si la Gateway déclare une fabrique
     * @param descriptors description d'une fabrique
     * @param regexFields arguments qui portent une expression régulière, par fabrique
     * @param optionalFields arguments facultatifs, par fabrique
     */
    private record StepKind(String target, Predicate<String> known, Function<String, FactoryDescriptor> descriptors,
            Map<String, Set<String>> regexFields, Map<String, Set<String>> optionalFields) {
    }
}
