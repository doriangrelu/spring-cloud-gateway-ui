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
import java.util.List;
import java.util.Map;

import io.github.doriangrelu.gatewayui.internal.editor.Advice.Level;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
import org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SetRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.CookieRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.HeaderRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.QueryRoutePredicateFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class RouteAdvisorTest {

    private static final EditableStep PATH = step("Path", "patterns", "/api/**");

    private final FactoryCatalog catalog = new FactoryCatalog(
            List.of(new PathRoutePredicateFactory(new WebFluxProperties()), new HeaderRoutePredicateFactory(),
                    new QueryRoutePredicateFactory(), new CookieRoutePredicateFactory()),
            List.of(new RewritePathGatewayFilterFactory(), new AddRequestHeaderGatewayFilterFactory(),
                    new SetRequestHeaderGatewayFilterFactory(), new StripPrefixGatewayFilterFactory()));

    private final RouteAdvisor advisor = new RouteAdvisor(catalog,
            value -> value.replace("${orders.url}", "http://orders.test").replace("${bad.uri}", "orders test"));

    @Test
    void validRouteHasNoAdvice() {
        final EditableRoute route = route("orders", "http://orders.test", List.of(PATH),
                List.of(step("StripPrefix", "parts", "1"), step("RewritePath", "regexp", "/api/(?<s>.*)", "replacement", "/$\\{s}")));

        assertThat(advisor.advise(route, List.of(route("users", "http://users.test", List.of(PATH), List.of())))).isEmpty();
    }

    @Test
    void blankIdentifierIsAnError() {
        assertThat(advise(route(" ", "http://orders.test", List.of(PATH), List.of())))
                .containsExactly(tuple(Level.ERROR, "id", "editor.advice.id.empty"));
    }

    @Test
    void identifierUsedByAnotherRouteIsAnError() {
        final List<Advice> advice = advisor.advise(route("orders", "http://orders.test", List.of(PATH), List.of()),
                List.of(route(" orders ", "http://other.test", List.of(PATH), List.of())));

        assertThat(advice).extracting(Advice::level, Advice::target, a -> a.message().key())
                .containsExactly(tuple(Level.ERROR, "id", "editor.advice.id.duplicate"));
        assertThat(advice.getFirst().message().args()).containsExactly("orders");
    }

    @Test
    void emptyUriIsAnError() {
        assertThat(advise(route("orders", "", List.of(PATH), List.of())))
                .containsExactly(tuple(Level.ERROR, "uri", "editor.advice.uri.empty"));
    }

    @Test
    void relativeOrOpaqueUriIsAnError() {
        assertThat(advise(route("orders", "/orders", List.of(PATH), List.of())))
                .containsExactly(tuple(Level.ERROR, "uri", "editor.advice.uri.relative"));
        assertThat(advise(route("orders", "orders.test:8080", List.of(PATH), List.of())))
                .containsExactly(tuple(Level.ERROR, "uri", "editor.advice.uri.relative"));
    }

    @Test
    void unparsableUriIsAnError() {
        assertThat(advise(route("orders", "http://orders test", List.of(PATH), List.of())))
                .containsExactly(tuple(Level.ERROR, "uri", "editor.advice.uri.invalid"));
    }

    @Test
    void uriPlaceholderIsResolvedBeforeValidation() {
        assertThat(advise(route("orders", "${orders.url}", List.of(PATH), List.of()))).isEmpty();
        assertThat(advise(route("orders", "${bad.uri}", List.of(PATH), List.of())))
                .containsExactly(tuple(Level.ERROR, "uri", "editor.advice.uri.invalid"));
    }

    @Test
    void unresolvedUriPlaceholderIsAccepted() {
        assertThat(advise(route("orders", "${deployment.url}", List.of(PATH), List.of()))).isEmpty();
    }

    @Test
    void unknownOrMissingFactoryIsAnError() {
        final EditableRoute route = route("orders", "http://orders.test", List.of(PATH, step("Nope", "a", "b")),
                List.of(step(" ", "a", "b")));

        assertThat(advise(route)).containsExactly(
                tuple(Level.ERROR, "predicates[1]", "editor.advice.factory.unknown"),
                tuple(Level.ERROR, "filters[0]", "editor.advice.factory.missing"));
    }

    @Test
    void emptyRequiredArgumentIsAnError() {
        final EditableRoute route = route("orders", "http://orders.test", List.of(step("Path", "patterns", " ")),
                List.of(step("AddRequestHeader", "name", "X-Env"), step("RewritePath", "regexp", "/a", "replacement", "")));

        assertThat(advise(route)).containsExactly(
                tuple(Level.ERROR, "predicates[0].patterns", "editor.advice.argument.empty"),
                tuple(Level.ERROR, "filters[0].value", "editor.advice.argument.empty"),
                tuple(Level.ERROR, "filters[1].replacement", "editor.advice.argument.empty"));
    }

    @Test
    void optionalArgumentsMayBeLeftEmpty() {
        final EditableRoute route = route("orders", "http://orders.test",
                List.of(step("Path", "patterns", "/a/**", "matchTrailingSlash", ""), step("Header", "header", "X-Env"),
                        step("Query", "param", "debug")),
                List.of(step("StripPrefix")));

        assertThat(advise(route)).isEmpty();
    }

    @Test
    void invalidRegularExpressionIsAnError() {
        final EditableRoute route = route("orders", "http://orders.test",
                List.of(PATH, step("Header", "header", "X-Env", "regexp", "[a-z"), step("Cookie", "name", "c", "regexp", "(")),
                List.of(step("RewritePath", "regexp", "/api/(?<s.*)", "replacement", "/$\\{s}")));

        assertThat(advise(route)).containsExactly(
                tuple(Level.ERROR, "predicates[1].regexp", "editor.advice.regex.invalid"),
                tuple(Level.ERROR, "predicates[2].regexp", "editor.advice.regex.invalid"),
                tuple(Level.ERROR, "filters[0].regexp", "editor.advice.regex.invalid"));
    }

    @Test
    void regularExpressionWithUnresolvedPlaceholderIsAccepted() {
        final EditableRoute route = route("orders", "http://orders.test",
                List.of(PATH, step("Query", "param", "v", "regexp", "${version.regexp}")), List.of());

        assertThat(advise(route)).isEmpty();
    }

    @Test
    void routeWithoutPredicateIsAWarning() {
        assertThat(advise(route("orders", "http://orders.test", List.of(), List.of())))
                .containsExactly(tuple(Level.WARNING, "predicates", "editor.advice.predicates.none"));
    }

    @Test
    void catchAllPathIsAWarning() {
        final EditableRoute route = route("orders", "http://orders.test", List.of(step("Path", "patterns", "/api/**, /**")), List.of());

        assertThat(advise(route)).containsExactly(tuple(Level.WARNING, "predicates[0].patterns", "editor.advice.predicates.catchAll"));
    }

    @Test
    void bearerTokenInClearIsAWarning() {
        final EditableRoute route = route("orders", "http://orders.test", List.of(PATH),
                List.of(step("AddRequestHeader", "name", "X-Forwarded-Auth", "value", "Bearer eyJhbGciOiJIUzI1NiJ9.abc")));

        assertThat(advise(route)).containsExactly(tuple(Level.WARNING, "filters[0].value", "editor.advice.secret"));
    }

    @Test
    void valueOfASensitiveHeaderIsAWarning() {
        final EditableRoute route = route("orders", "http://orders.test", List.of(PATH),
                List.of(step("SetRequestHeader", "name", "X-Api-Key", "value", "s3cr3t"),
                        step("AddRequestHeader", "name", "Authorization", "value", "${api.token}")));

        assertThat(advise(route)).containsExactly(tuple(Level.WARNING, "filters[0].value", "editor.advice.secret"));
    }

    @Test
    void sensitiveFieldOfACustomFactoryIsAWarning() {
        final FactoryCatalog empty = new FactoryCatalog(List.of(new HeaderRoutePredicateFactory()), List.of());
        final EditableRoute route = route("orders", "http://orders.test", List.of(step("Header", "header", "X-Env")),
                List.of(step("Custom", "password", "hunter2", "retries", "3")));

        assertThat(new RouteAdvisor(empty, value -> value).advise(route, List.of()))
                .extracting(Advice::level, Advice::target, a -> a.message().key())
                .containsExactly(tuple(Level.ERROR, "filters[0]", "editor.advice.factory.unknown"),
                        tuple(Level.WARNING, "filters[0].password", "editor.advice.secret"));
    }

    @Test
    void passwordInUriIsAWarning() {
        assertThat(advise(route("orders", "http://admin:s3cr3t@orders.test", List.of(PATH), List.of())))
                .containsExactly(tuple(Level.WARNING, "uri", "editor.advice.secret"));
    }

    @Test
    void regularExpressionsAndHeaderNamesAreNotSecrets() {
        final EditableRoute route = route("orders", "http://orders.test",
                List.of(PATH, step("Header", "header", "Authorization", "regexp", "Bearer .+")),
                List.of(step("AddRequestHeader", "name", "X-Token-Count", "value", "3")));

        assertThat(advise(route)).isEmpty();
    }

    private List<Tuple> advise(final EditableRoute route) {
        return advisor.advise(route, List.of()).stream()
                .map(advice -> tuple(advice.level(), advice.target(), advice.message().key()))
                .toList();
    }

    private static EditableRoute route(final String id, final String uri, final List<EditableStep> predicates,
            final List<EditableStep> filters) {
        return new EditableRoute(id, uri, 0, predicates, filters);
    }

    private static EditableStep step(final String name, final String... keyValues) {
        final Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            values.put(keyValues[i], keyValues[i + 1]);
        }
        return new EditableStep(name, values);
    }
}
