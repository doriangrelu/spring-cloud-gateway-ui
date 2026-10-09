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
package io.github.doriangrelu.gatewayui.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Gateway d'exemple : quelques routes représentatives pour explorer l'UI sur http://localhost:8080/gateway-ui.
 */
@SpringBootApplication
public class SampleGatewayApplication {

    /**
     * Démarre la Gateway d'exemple.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(final String[] args) {
        SpringApplication.run(SampleGatewayApplication.class, args);
    }

    /** Route déclarée en Java, pour voir ce que l'UI peut en montrer. */
    @Bean
    RouteLocator javaRoutes(final RouteLocatorBuilder builder) {
        return builder.routes()
                .route("legacy-java", route -> route.path("/old/**")
                        .filters(filter -> filter.rewritePath("/old/(?<rest>.*)", "/v1/${rest}"))
                        .uri("http://legacy.shop.svc.cluster.local:8080"))
                .build();
    }

    /**
     * Piège volontaire : un {@code @Order} sur la méthode {@code @Bean} est ignoré par la Gateway, l'UI affiche ce
     * filtre « non ordonné ».
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    GlobalFilter correlationIdFilter() {
        return (exchange, chain) -> chain.filter(exchange);
    }
}
