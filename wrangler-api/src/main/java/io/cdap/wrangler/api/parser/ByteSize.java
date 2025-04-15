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
 * Represents a byte size value with parsing capabilities from string tokens
 * (e.g., "1MB", "10KB").
 * Implements the Token interface to provide a standardized way to access the
 * byte value
 * and convert it to JSON.
 */
public class ByteSize implements Token {
    private final long bytes;

    public ByteSize(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Byte size cannot be empty");
        }
        this.bytes = parseByteSize(token);
    }

    private long parseByteSize(String token) {
        String[] parts = token.split("(?i)[kKmMgGtT][bB]");
        if (parts.length != 1) {
            throw new IllegalArgumentException("Invalid byte size format: " + token);
        }
        double value;
        try {
            value = Double.parseDouble(parts[0].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value in byte size: " + token, e);
        }
        if (value < 0) {
            throw new IllegalArgumentException("Byte size cannot be negative: " + token);
        }
        String unit = token.replaceAll("[0-9.]", "").toUpperCase();

        switch (unit) {
            case "KB":
                return (long) (value * 1024);
            case "MB":
                return (long) (value * 1024 * 1024);
            case "GB":
                return (long) (value * 1024 * 1024 * 1024);
            case "TB":
                return (long) (value * 1024 * 1024 * 1024 * 1024);
            default:
                throw new IllegalArgumentException("Invalid byte size unit: " + token);
        }
    }

    public long getBytes() {
        return bytes;
    }

    @Override
    public Object value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("value", bytes);
        json.addProperty("unit", "bytes");
        return json;
    }
}
