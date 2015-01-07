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

package com.tortel.authenticator;

/**
 * Indicates that {@link OtpSource} failed to generate an OTP because OTP generation is not
 * permitted for the specified account.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class OtpGenerationNotPermittedException extends OtpSourceException {
    private static final long serialVersionUID = 822429897995102097L;

    public OtpGenerationNotPermittedException(String message) {
        super(message);
    }
}