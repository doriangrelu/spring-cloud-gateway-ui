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
package io.github.doriangrelu.gatewayui.internal.i18n;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesTest {

    private final Messages messages = new Messages();

    @Test
    void everyLanguageHasExactlyTheSameKeys() {
        assertThat(messages.keys(Locale.FRENCH)).isEqualTo(messages.keys(Locale.ENGLISH));
    }

    @Test
    void formatsArgumentsAndApostrophes() {
        assertThat(messages.text(Locale.FRENCH, Message.of("sim.note.loadBalancer", "orders")))
                .isEqualTo("URI lb:// : l'hôte final est choisi par le load balancer parmi les instances de « orders ».");
        assertThat(messages.text(Locale.ENGLISH, "route.meta.order", 10)).isEqualTo("Order 10");
    }

    @Test
    void keepsApostrophesOfMessagesWithoutArguments() {
        assertThat(messages.text(Locale.FRENCH, "routes.intro")).startsWith("Dans l'ordre d'évaluation");
    }

    @Test
    void acceptsNullArguments() {
        assertThat(messages.text(Locale.ENGLISH, Message.of("sim.failed", (Object) null))).isEqualTo("Simulation failed: null");
    }

    @Test
    void unknownKeyIsShownBetweenBrackets() {
        assertThat(messages.text(Locale.ENGLISH, "does.not.exist")).isEqualTo("[does.not.exist]");
    }

    @Test
    void matchesSupportedLanguagesByLanguageOnly() {
        assertThat(Messages.supported(Locale.CANADA_FRENCH)).contains(Locale.FRENCH);
        assertThat(Messages.supported(Locale.GERMAN)).isEmpty();
        assertThat(Messages.supported(null)).isEmpty();
    }
}
