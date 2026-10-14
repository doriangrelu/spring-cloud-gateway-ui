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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.MediaType;

import io.netty.buffer.PooledByteBufAllocator;

import static org.assertj.core.api.Assertions.assertThat;

class BodyExcerptReaderTest {

    private static final DefaultDataBufferFactory BUFFERS = DefaultDataBufferFactory.sharedInstance;

    @Test
    void readsAShortTextBodyEntirely() {
        final BodyExcerpt excerpt = new BodyExcerptReader(64).read(body("{\"id\": 42}"), MediaType.APPLICATION_JSON).block();

        assertThat(excerpt.text()).isEqualTo("{\"id\": 42}");
        assertThat(excerpt.truncated()).isFalse();
        assertThat(excerpt.binary()).isFalse();
        assertThat(excerpt.receivedBytes()).isEqualTo(10);
    }

    @Test
    void truncatesALongBody() {
        final BodyExcerpt excerpt = new BodyExcerptReader(4).read(body("abc", "def", "ghi"), MediaType.TEXT_PLAIN).block();

        assertThat(excerpt.text()).isEqualTo("abcd");
        assertThat(excerpt.truncated()).isTrue();
    }

    @Test
    void doesNotShowABinaryBody() {
        final BodyExcerpt excerpt = new BodyExcerptReader(64).read(body("PNG..."), MediaType.IMAGE_PNG).block();

        assertThat(excerpt.binary()).isTrue();
        assertThat(excerpt.text()).isEmpty();
        assertThat(excerpt.receivedBytes()).isEqualTo(6);
    }

    @Test
    void usesTheDeclaredCharsetAndUtf8ByDefault() {
        final Flux<DataBuffer> latin1 = Flux.just(BUFFERS.wrap("é".getBytes(StandardCharsets.ISO_8859_1)));

        assertThat(new BodyExcerptReader(64).read(latin1, MediaType.parseMediaType("text/plain;charset=ISO-8859-1"))
                .block().text()).isEqualTo("é");
        assertThat(new BodyExcerptReader(64).read(body("é"), null).block().text()).isEqualTo("é");
        assertThat(new BodyExcerptReader(64).read(body("<a/>"), MediaType.APPLICATION_XML).block().binary()).isFalse();
    }

    @Test
    void reportsAnEmptyBody() {
        final BodyExcerpt excerpt = new BodyExcerptReader(64).read(Flux.empty(), MediaType.TEXT_PLAIN).block();

        assertThat(excerpt.receivedBytes()).isZero();
        assertThat(excerpt.text()).isEmpty();
    }

    @Test
    void releasesTheBuffersItReads() {
        final NettyDataBufferFactory pooled = new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);
        final List<PooledDataBuffer> buffers = new ArrayList<>();
        for (final String chunk : List.of("abc", "def")) {
            final DataBuffer buffer = pooled.allocateBuffer(chunk.length()).write(chunk.getBytes(StandardCharsets.UTF_8));
            buffers.add((PooledDataBuffer) buffer);
        }

        new BodyExcerptReader(64).read(Flux.fromIterable(buffers), MediaType.TEXT_PLAIN).block();

        assertThat(buffers).noneMatch(PooledDataBuffer::isAllocated);
    }

    private static Flux<DataBuffer> body(final String... chunks) {
        return Flux.fromArray(chunks).map(chunk -> BUFFERS.wrap(chunk.getBytes(StandardCharsets.UTF_8)));
    }
}
