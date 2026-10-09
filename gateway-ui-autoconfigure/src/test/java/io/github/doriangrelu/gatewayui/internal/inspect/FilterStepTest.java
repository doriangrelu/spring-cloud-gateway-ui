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
package io.github.doriangrelu.gatewayui.internal.inspect;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

class FilterStepTest {

    @Test
    void describesFactoryFilterFromItsToString() {
        final StripPrefixGatewayFilterFactory.Config config = new StripPrefixGatewayFilterFactory.Config();
        config.setParts(2);
        final GatewayFilter filter = new OrderedGatewayFilter(new StripPrefixGatewayFilterFactory().apply(config), 1);

        final FilterStep step = FilterStep.ofRoute(filter);

        assertThat(step.name()).isEqualTo("StripPrefix");
        assertThat(step.description()).isEqualTo("parts = 2");
        assertThat(step.order()).isEqualTo(1);
        assertThat(step.global()).isFalse();
    }

    @Test
    void namesLambdaGlobalFilterAfterItsBean() {
        final GlobalFilter lambda = (exchange, chain) -> chain.filter(exchange);

        final FilterStep step = FilterStep.ofGlobal(lambda, "correlationIdFilter");

        assertThat(step.name()).isEqualTo("correlationIdFilter");
        assertThat(step.ordered()).isFalse();
        assertThat(step.lambda()).isTrue();
        assertThat(step.description()).isEqualTo(FilterStepTest.class.getName());
    }

    @Test
    void writesOrdersNearBoundsRelatively() {
        assertThat(ordered(Ordered.HIGHEST_PRECEDENCE).orderText()).isEqualTo("HIGHEST");
        assertThat(ordered(Ordered.HIGHEST_PRECEDENCE + 1000).orderText()).isEqualTo("HIGHEST+1000");
        assertThat(ordered(Ordered.LOWEST_PRECEDENCE - 1).orderText()).isEqualTo("LOWEST-1");
        assertThat(ordered(10_150).orderText()).isEqualTo("10150");
    }

    private static FilterStep ordered(final int order) {
        return new FilterStep("Filter", "", order, true, FilterStep.Scope.ROUTE, false);
    }
}
