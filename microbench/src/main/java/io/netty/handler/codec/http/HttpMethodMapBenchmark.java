/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http;

import io.netty.microbench.util.AbstractMicrobenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpMethod.CONNECT;
import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.HEAD;
import static io.netty.handler.codec.http.HttpMethod.OPTIONS;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpMethod.TRACE;
import static io.netty.util.internal.MathUtil.findNextPositivePowerOfTwo;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 8)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class HttpMethodMapBenchmark extends AbstractMicrobenchmark {
    private static final Map<String, HttpMethod> OLD_MAP = new HashMap<>();
    private static final SimpleStringMap<HttpMethod> NEW_MAP;
    private static final String[] KNOWN_METHODS;
    private static final String[] MIXED_METHODS;
    private static final String[] UNKNOWN_METHODS;

    static {
        // We intentionally don't use HttpMethod.toString() here to avoid the equals(..) comparison method from being
        // able to short circuit due to reference equality checks and being biased toward the new approach. This
        // simulates the behavior of HttpObjectDecoder which will build new String objects during the decode operation.
        KNOWN_METHODS = new String[] {
                "OPTIONS",
                "GET",
                "HEAD",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "TRACE",
                "CONNECT"
        };
        MIXED_METHODS = new String[] {
                "OPTIONS",
                "FAKEMETHOD",
                "GET",
                "HEAD",
                "POST",
                "UBERGET",
                "PUT",
                "PATCH",
                "MYMETHOD",
                "DELETE",
                "TRACE",
                "CONNECT",
                "WHATMETHOD"
        };
        UNKNOWN_METHODS = new String[] {
                "FAKEMETHOD",
                "UBERGET",
                "MYMETHOD",
                "TESTING",
                "WHATMETHOD",
                "UNKNOWN",
                "FOOBAR"
        };
        OLD_MAP.put(OPTIONS.toString(), OPTIONS);
        OLD_MAP.put(GET.toString(), GET);
        OLD_MAP.put(HEAD.toString(), HEAD);
        OLD_MAP.put(POST.toString(), POST);
        OLD_MAP.put(PUT.toString(), PUT);
        OLD_MAP.put(PATCH.toString(), PATCH);
        OLD_MAP.put(DELETE.toString(), DELETE);
        OLD_MAP.put(TRACE.toString(), TRACE);
        OLD_MAP.put(CONNECT.toString(), CONNECT);

        NEW_MAP = new SimpleStringMap<>(
                new SimpleStringMap.Node<>(OPTIONS.toString(), OPTIONS),
                new SimpleStringMap.Node<>(GET.toString(), GET),
                new SimpleStringMap.Node<>(HEAD.toString(), HEAD),
                new SimpleStringMap.Node<>(POST.toString(), POST),
                new SimpleStringMap.Node<>(PUT.toString(), PUT),
                new SimpleStringMap.Node<>(PATCH.toString(), PATCH),
                new SimpleStringMap.Node<>(DELETE.toString(), DELETE),
                new SimpleStringMap.Node<>(TRACE.toString(), TRACE),
                new SimpleStringMap.Node<>(CONNECT.toString(), CONNECT));
    }

    private static final class SimpleStringMap<T> {
        private final SimpleStringMap.Node<T>[] values;
        private final int valuesMask;

        SimpleStringMap(SimpleStringMap.Node<T>... nodes) {
            values = (SimpleStringMap.Node<T>[]) new SimpleStringMap.Node[findNextPositivePowerOfTwo(nodes.length)];
            valuesMask = values.length - 1;
            for (SimpleStringMap.Node<T> node : nodes) {
                int i = hashCode(node.key) & valuesMask;
                if (values[i] != null) {
                    throw new IllegalArgumentException("index " + i + " collision between values: [" +
                            values[i].key + ", " + node.key + "]");
                }
                values[i] = node;
            }
        }

        T get(String name) {
            SimpleStringMap.Node<T> node = values[hashCode(name) & valuesMask];
            return node == null || !node.key.equals(name) ? null : node.value;
        }

        private static int hashCode(String name) {
            // This hash code needs to produce a unique index for each HttpMethod. If new methods are added this
            // algorithm will need to be adjusted. The goal is to have each enum name's hash value correlate to a unique
            // index in the values array.
            return name.hashCode() >>> 6;
        }

        private static final class Node<T> {
            final String key;
            final T value;

            Node(String key, T value) {
                this.key = key;
                this.value = value;
            }
        }
    }

    @Benchmark
    public int oldMapKnownMethods() throws Exception {
        int x = 0;
        for (int i = 0; i < KNOWN_METHODS.length; ++i) {
            x += OLD_MAP.get(KNOWN_METHODS[i]).toString().length();
        }
        return x;
    }

    @Benchmark
    public int newMapKnownMethods() throws Exception {
        int x = 0;
        for (int i = 0; i < KNOWN_METHODS.length; ++i) {
            x += NEW_MAP.get(KNOWN_METHODS[i]).toString().length();
        }
        return x;
    }

    @Benchmark
    public int oldMapMixMethods() throws Exception {
        int x = 0;
        for (int i = 0; i < MIXED_METHODS.length; ++i) {
            HttpMethod method = OLD_MAP.get(MIXED_METHODS[i]);
            if (method != null) {
                x += method.toString().length();
            }
        }
        return x;
    }

    @Benchmark
    public int newMapMixMethods() throws Exception {
        int x = 0;
        for (int i = 0; i < MIXED_METHODS.length; ++i) {
            HttpMethod method = NEW_MAP.get(MIXED_METHODS[i]);
            if (method != null) {
                x += method.toString().length();
            }
        }
        return x;
    }

    @Benchmark
    public int oldMapUnknownMethods() throws Exception {
        int x = 0;
        for (int i = 0; i < UNKNOWN_METHODS.length; ++i) {
            HttpMethod method = OLD_MAP.get(UNKNOWN_METHODS[i]);
            if (method != null) {
                x += method.toString().length();
            }
        }
        return x;
    }

    @Benchmark
    public int newMapUnknownMethods() throws Exception {
        int x = 0;
        for (int i = 0; i < UNKNOWN_METHODS.length; ++i) {
            HttpMethod method = NEW_MAP.get(UNKNOWN_METHODS[i]);
            if (method != null) {
                x += method.toString().length();
            }
        }
        return x;
    }
}
