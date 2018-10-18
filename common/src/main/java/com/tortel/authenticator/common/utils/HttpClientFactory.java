/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tortel.authenticator.common.utils;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Factory that creates an {@link OkHttpClient}.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
final class HttpClientFactory {

    /**
     * Timeout (ms) for establishing a connection.
     */
    // @VisibleForTesting
    static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 20 * 1000;

    /**
     * Timeout (ms) for read operations on connections.
     */
    // @VisibleForTesting
    static final int DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000;

    /**
     * Timeout (ms) for obtaining a connection from the connection pool.
     */
    // @VisibleForTesting
    static final int DEFAULT_GET_CONNECTION_FROM_POOL_TIMEOUT_MILLIS = 20 * 1000;

    /**
     * Hidden constructor to prevent instantiation.
     */
    private HttpClientFactory() {
    }

    /**
     * Creates a new {@link OkHttpClient}.
     *
     * @param context context for reusing SSL sessions.
     */
    static OkHttpClient createHttpClient(Context context) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .build();

        return client;
    }

}
