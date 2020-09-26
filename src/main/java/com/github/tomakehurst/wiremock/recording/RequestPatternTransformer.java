/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.recording;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.*;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Creates a RequestPatternBuilder from a Request's URL, method, body (if
 * present), and optionally headers from a whitelist.
 */
public class RequestPatternTransformer implements Function<Request, RequestPatternBuilder> {

    private final List<StringValuePattern> headersToMatch;
    private final RequestBodyPatternFactory bodyPatternFactory;

    public RequestPatternTransformer(List<StringValuePattern> headersToMatch, RequestBodyPatternFactory bodyPatternFactory) {
        this.headersToMatch = headersToMatch;
        this.bodyPatternFactory = bodyPatternFactory;
    }

    /**
     * Returns a RequestPatternBuilder matching a given Request
     */
    @Override
    public RequestPatternBuilder apply(final Request request) {
        final RequestPatternBuilder builder = new RequestPatternBuilder(request.getMethod(),
                urlEqualTo(request.getUrl()));

        if (headersToMatch != null && !headersToMatch.isEmpty()) {
            List<StringValuePattern> headersToIgnore = newArrayList();
            if (request.isMultipart()) {
                headersToIgnore.add(new EqualToPattern("Content-Type", true));
            }

            for (final HttpHeader header : request.getHeaders().all()) {
                Predicate<StringValuePattern> predicate = new Predicate<StringValuePattern>() {
                    @Override
                    public boolean apply(StringValuePattern pattern) {
                        return pattern.match(header.key()).isExactMatch();
                    }
                };
                if (from(headersToMatch).anyMatch(predicate) && !from(headersToIgnore).anyMatch(predicate)) {
                    builder.withHeader(header.key(), equalTo(header.firstValue()));
                }
            }
        }

        byte[] body = request.getBody();
        if (bodyPatternFactory != null && body != null && body.length > 0) {
            builder.withRequestBody(bodyPatternFactory.forRequest(request));
        }

        return builder;
    }
}
