/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Represents a time duration value with parsing capabilities from string tokens
 * (e.g., "1s", "500ms").
 * Implements the Token interface to provide a standardized way to access the
 * duration in nanoseconds
 * and convert it to JSON.
 */
public class TimeDuration implements Token {
    private final long nanos;

    public TimeDuration(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Time duration cannot be empty");
        }
        this.nanos = parseTimeDuration(token);
    }

    private long parseTimeDuration(String token) {
        String[] parts = token.split("(?i)[nNuUmMsShH]");
        if (parts.length != 1 || parts[0].trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid time duration format: " + token);
        }
        double value;
        try {
            value = Double.parseDouble(parts[0].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value in time duration: " + token, e);
        }
        if (value < 0) {
            throw new IllegalArgumentException("Time duration cannot be negative: " + token);
        }
        String unit = token.replaceAll("[0-9.]", "").toLowerCase();

        switch (unit) {
            case "ns":
                return (long) (value);
            case "us":
                return (long) (value * 1000);
            case "ms":
                return (long) (value * 1000000);
            case "s":
                return (long) (value * 1000000000);
            case "m":
                return (long) (value * 1000000000 * 60);
            case "h":
                return (long) (value * 1000000000 * 60 * 60);
            default:
                throw new IllegalArgumentException("Invalid time unit: " + token);
        }
    }

    public long getNanos() {
        return nanos;
    }

    @Override
    public Object value() {
        return nanos;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("value", nanos);
        json.addProperty("unit", "ns");
        return json;
    }
}
