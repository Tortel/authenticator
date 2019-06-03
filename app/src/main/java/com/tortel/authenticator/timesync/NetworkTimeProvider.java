/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.tortel.authenticator.timesync;

import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Provider of network time that obtains the time by making a network request to Google.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class NetworkTimeProvider {

    private static final String LOG_TAG = NetworkTimeProvider.class.getSimpleName();
    private static final String URL = "https://www.google.com";
    private static final String DATE_FORMAT = "EEE, dd LLL yyyy HH:mm:ss zzz";

    private final OkHttpClient mHttpClient;

    public NetworkTimeProvider(OkHttpClient httpClient) {
        mHttpClient = httpClient;
    }

    /**
     * Gets the system time by issuing a request over the network.
     *
     * @return time (milliseconds since epoch).
     * @throws IOException if an I/O error occurs.
     */
    public long getNetworkTime() throws IOException {
        Request request = new Request.Builder()
                .url(URL)
                .build();
        Log.i(LOG_TAG, "Sending request to " + request.url().uri());

        try (Response response = mHttpClient.newCall(request).execute()) {
            String dateHeader = response.header("Date");
            Log.i(LOG_TAG, "Received response with Date header: " + dateHeader);
            if (dateHeader == null) {
                throw new IOException("No Date header in response");
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
                Date networkDate = sdf.parse(dateHeader);
                return networkDate.getTime();
            } catch (ParseException e) {
                throw new IOException(
                        "Invalid Date header format in response: \"" + dateHeader + "\"");
            }
        }
    }
}
