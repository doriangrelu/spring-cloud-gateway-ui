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
package io.github.doriangrelu.gatewayui.internal.realcall;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Lit le début du corps d'une réponse, sans jamais dépasser la limite configurée : au-delà, la lecture est annulée, quel
 * que soit le service appelé.
 */
class BodyExcerptReader {

    private final int maxBytes;

    BodyExcerptReader(final int maxBytes) {
        this.maxBytes = maxBytes;
    }

    Mono<BodyExcerpt> read(final Flux<DataBuffer> body, final MediaType contentType) {
        return Mono.defer(() -> {
            final Accumulator accumulator = new Accumulator(maxBytes);
            // takeUntil annule le flux dès la limite atteinte : le reste du corps n'est jamais lu
            return body.doOnNext(accumulator::add)
                    .takeUntil(buffer -> accumulator.full())
                    .then(Mono.fromSupplier(() -> accumulator.excerpt(contentType)));
        });
    }

    /** Sans type déclaré, le corps est supposé textuel : c'est le cas le plus courant pour une API. */
    private static boolean isText(final MediaType type) {
        if (type == null) {
            return true;
        }
        final String subtype = type.getSubtype();
        return "text".equals(type.getType()) || subtype.contains("json") || subtype.contains("xml")
                || MediaType.APPLICATION_FORM_URLENCODED.equalsTypeAndSubtype(type) || subtype.contains("javascript");
    }

    /**
     * Octets lus, dans la limite : état d'une lecture.
     */
    private static class Accumulator {

        private final int maxBytes;

        private final ByteArrayOutputStream kept = new ByteArrayOutputStream();

        private long received;

        Accumulator(final int maxBytes) {
            this.maxBytes = maxBytes;
        }

        void add(final DataBuffer buffer) {
            try {
                final int readable = buffer.readableByteCount();
                final byte[] bytes = new byte[Math.min(readable, Math.max(0, maxBytes - kept.size()))];
                buffer.read(bytes);
                kept.writeBytes(bytes);
                received += readable;
            }
            finally {
                DataBufferUtils.release(buffer);
            }
        }

        boolean full() {
            return received > maxBytes;
        }

        BodyExcerpt excerpt(final MediaType contentType) {
            if (!isText(contentType)) {
                return new BodyExcerpt("", received, full(), true);
            }
            final Charset charset = contentType == null || contentType.getCharset() == null
                    ? StandardCharsets.UTF_8 : contentType.getCharset();
            return new BodyExcerpt(kept.toString(charset), received, full(), false);
        }
    }
}
