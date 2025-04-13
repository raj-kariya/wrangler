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
 * The TimeDuration class represents a duration with support for different time units (ms, s, min, h, d).
 * It parses strings like "150ms", "30s", "5min" etc. and provides methods to get the value in nanoseconds.
 */
@PublicEvolving
public class TimeDuration implements Token {
  private final String originalValue;
  private final long nanoseconds;

  /**
   * Constructs a TimeDuration by parsing a string representation.
   * Supported formats: number followed by ms, s, min, h, or d (case sensitive)
   * Examples: "150ms", "30s", "5min", "2h", "1d"
   *
   * @param value the string representation of time duration
   * @throws IllegalArgumentException if the format is invalid
   */
  public TimeDuration(String value) {
    this.originalValue = value;
    this.nanoseconds = parseTimeDuration(value);
  }

  private long parseTimeDuration(String value) {
    if (value == null || value.trim().isEmpty()) {
        throw new IllegalArgumentException("TimeDuration value cannot be null or empty");
    }

    // Remove underscores and parse scientific notation
    String normalizedValue = value.replace("_", "");
    
    // Match pattern: number + unit
    Matcher matcher = Pattern.compile("^([-0-9.eE]+)(ms|s|min|h|d)$").matcher(normalizedValue);
    if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid time duration format: " + value);
    }

    // Parse the number part (supports scientific notation)
    double number = Double.parseDouble(matcher.group(1));
    String unit = matcher.group(2);

    // Convert to nanoseconds based on unit
    switch (unit) {
        case "ms": return (long) (number * 1_000_000);
        case "s": return (long) (number * 1_000_000_000);
        case "min": return (long) (number * 60 * 1_000_000_000L);
        case "h": return (long) (number * 3600 * 1_000_000_000L);
        case "d": return (long) (number * 24 * 3600 * 1_000_000_000L);
        default: throw new IllegalArgumentException("Invalid unit: " + unit);
    }
  }

  /**
   * Returns the duration in nanoseconds.
   *
   * @return the duration in nanoseconds
   */
  public long getNanoSeconds() {
    return nanoseconds;
  }

  @Override
  public String value() {
    return originalValue;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.TIME_DURATION.name());
    object.addProperty("value", originalValue);
    object.addProperty("nanoseconds", nanoseconds);
    return object;
  }

  @Override
  public String toString() {
    return originalValue;
  }
}
