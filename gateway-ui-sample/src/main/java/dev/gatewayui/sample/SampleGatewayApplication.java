package dev.gatewayui.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@SpringBootApplication
public class SampleGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleGatewayApplication.class, args);
    }

    /** Route déclarée en Java, pour voir ce que l'UI peut en montrer. */
    @Bean
    RouteLocator javaRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("legacy-java", route -> route.path("/old/**")
                        .filters(filter -> filter.rewritePath("/old/(?<rest>.*)", "/v1/${rest}"))
                        .uri("http://legacy.shop.svc.cluster.local:8080"))
                .build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    GlobalFilter correlationIdFilter() {
        return (exchange, chain) -> chain.filter(exchange);
    }
}
