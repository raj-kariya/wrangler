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

import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The ByteSize class represents a size in bytes with support for different units (B, KB, MB, GB, TB).
 * It parses strings like "10KB", "2MB", "1GB" etc. and provides methods to get the value in bytes.
 */
@PublicEvolving
public class ByteSize implements Token {
  private final String originalValue;
  private final long bytes;

  /**
   * Constructs a ByteSize by parsing a string representation.
   * Supported formats: number followed by B, KB, MB, GB, or TB (case sensitive)
   * Examples: "1024B", "10KB", "5MB", "2GB", "1TB"
   *
   * @param value the string representation of byte size
   * @throws IllegalArgumentException if the format is invalid
   */
  public ByteSize(String value) {
    this.originalValue = value;
    this.bytes = parseByteSize(value);
  }

  private long parseByteSize(String value) {
    if (value == null || value.trim().isEmpty()) {
        throw new IllegalArgumentException("ByteSize value cannot be null or empty");
    }

    // Remove underscores and parse scientific notation
    String normalizedValue = value.replace("_", "");
    
    // Match pattern: number + unit
    Matcher matcher = Pattern.compile("^([-0-9.eE]+)(B|KB|MB|GB|TB)$").matcher(normalizedValue);
    if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid byte size format: " + value);
    }

    // Parse the number part (supports scientific notation)
    double number = Double.parseDouble(matcher.group(1));
    String unit = matcher.group(2);

    // Convert to bytes based on unit
    switch (unit) {
        case "B": return (long) number;
        case "KB": return (long) (number * 1024);
        case "MB": return (long) (number * 1024 * 1024);
        case "GB": return (long) (number * 1024 * 1024 * 1024);
        case "TB": return (long) (number * 1024L * 1024L * 1024L * 1024L);
        default: throw new IllegalArgumentException("Invalid unit: " + unit);
    }
  }
  /**
   * Returns the size in bytes.
   *
   * @return the size in bytes
   */
  public long getBytes() {
    return bytes;
  }

  @Override
  public String value() {
    return originalValue;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.BYTE_SIZE.name());
    object.addProperty("value", originalValue);
    object.addProperty("bytes", bytes);
    return object;
  }

  @Override
  public String toString() {
    return originalValue;
  }
}
