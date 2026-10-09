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
package io.github.doriangrelu.gatewayui;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Gateway de test commune aux tests d'intégration : routes déclarées en configuration, avec placeholders, route
 * attrape-tout et filtre par défaut. Une configuration identique permet à Spring de partager le contexte.
 */
@SpringBootTest(classes = GatewayUiIntegrationTestSupport.TestGateway.class, properties = {
        "gateway.ui.enabled=true",
        "gateway.ui.services.billing.url=http://billing.test:8080",
        "gateway.ui.services.billing.display-name=Facturation",
        "spring.cloud.gateway.server.webflux.routes[0].id=orders",
        "orders.url=http://orders.test:8080",
        "spring.cloud.gateway.server.webflux.routes[0].uri=${orders.url}",
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/orders/**",
        "spring.cloud.gateway.server.webflux.routes[0].filters[0]=StripPrefix=1",
        "spring.cloud.gateway.server.webflux.routes[1].id=users",
        "spring.cloud.gateway.server.webflux.routes[1].uri=http://users.test:8080",
        "spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/users/{id}",
        "spring.cloud.gateway.server.webflux.routes[1].filters[0]=SetPath=/v2/users/{id}",
        // Route attrape-tout : ne doit masquer ni les pages ni les ressources de l'UI
        "spring.cloud.gateway.server.webflux.routes[2].id=catch-all",
        "spring.cloud.gateway.server.webflux.routes[2].uri=http://fallback.test:8080",
        "spring.cloud.gateway.server.webflux.routes[2].order=100",
        "spring.cloud.gateway.server.webflux.routes[2].predicates[0]=Path=/**",
        // Route qui répond sans appel réseau : ses réponses ne doivent pas porter les en-têtes de l'UI
        "spring.cloud.gateway.server.webflux.routes[3].id=no-op",
        "spring.cloud.gateway.server.webflux.routes[3].uri=no://op",
        "spring.cloud.gateway.server.webflux.routes[3].predicates[0]=Path=/no-op",
        "spring.cloud.gateway.server.webflux.routes[3].filters[0]=SetStatus=204",
        // Placeholders dans les arguments, en forme raccourcie et développée : l'éditeur doit les rétablir
        "spring.cloud.gateway.server.webflux.routes[4].id=secured",
        "spring.cloud.gateway.server.webflux.routes[4].uri=http://secured.test:8080",
        "spring.cloud.gateway.server.webflux.routes[4].predicates[0]=Path=/secured/**",
        "spring.cloud.gateway.server.webflux.routes[4].filters[0]=AddRequestHeader=Authorization, Bearer ${api.token:dev-token}",
        "spring.cloud.gateway.server.webflux.routes[4].filters[1].name=SetRequestHeader",
        "spring.cloud.gateway.server.webflux.routes[4].filters[1].args.name=X-Env",
        "spring.cloud.gateway.server.webflux.routes[4].filters[1].args.value=${env.name:dev}",
        // Filtre par défaut : la simulation de la route éditée doit le rejouer
        "spring.cloud.gateway.server.webflux.default-filters[0]=AddRequestHeader=X-Gateway, ui",
})
abstract class GatewayUiIntegrationTestSupport {

    @Autowired
    private ApplicationContext context;

    protected WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestGateway {
    }
}
