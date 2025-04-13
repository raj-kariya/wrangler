/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package io.cdap.wrangler.api.parser;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TimeDurationTest {

    @Test
    public void testValidTimeDurations() {
        // Test basic units
        assertEquals(1_000_000L, new TimeDuration("1ms").getNanoSeconds());
        assertEquals(1_000_000_000L, new TimeDuration("1s").getNanoSeconds());
        assertEquals(60L * 1_000_000_000L, new TimeDuration("1min").getNanoSeconds());
        assertEquals(3600L * 1_000_000_000L, new TimeDuration("1h").getNanoSeconds());
        assertEquals(24L * 3600L * 1_000_000_000L, new TimeDuration("1d").getNanoSeconds());
        
        // Test larger numbers and decimals
        assertEquals(2_100_000_000L, new TimeDuration("2.1s").getNanoSeconds());
        assertEquals(5_000_000L, new TimeDuration("5ms").getNanoSeconds());
        assertEquals(90L * 1_000_000_000L, new TimeDuration("1.5min").getNanoSeconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new TimeDuration("1w"); // Invalid unit
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat() {
        new TimeDuration("abc"); // Invalid format
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullInput() {
        new TimeDuration(null);
    }

    @Test
    public void testOriginalValue() {
        TimeDuration duration = new TimeDuration("1.5min");
        assertEquals("1.5min", duration.value());
    }

    @Test
    public void testTokenType() {
        TimeDuration duration = new TimeDuration("1s");
        assertEquals(TokenType.TIME_DURATION, duration.type());
    }

    @Test
    public void testNegativeDurations() {
        assertEquals(-1_000_000_000L, new TimeDuration("-1s").getNanoSeconds());
        assertEquals(-5_000_000L, new TimeDuration("-5ms").getNanoSeconds());
    }
}
