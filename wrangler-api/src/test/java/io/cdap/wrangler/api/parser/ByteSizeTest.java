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

public class ByteSizeTest {

    @Test
    public void testValidByteSizes() {
        // Test basic units
        assertEquals(1024L, new ByteSize("1KB").getBytes());
        assertEquals(1024L * 1024L, new ByteSize("1MB").getBytes());
        assertEquals(1024L * 1024L * 1024L, new ByteSize("1GB").getBytes());
        assertEquals(1024L * 1024L * 1024L * 1024L, new ByteSize("1TB").getBytes());
        
        // Test larger numbers
        assertEquals(10240L, new ByteSize("10KB").getBytes());
        assertEquals(1536L * 1024L * 1024L, new ByteSize("1.5GB").getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new ByteSize("1PB"); // Invalid unit
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat() {
        new ByteSize("abc"); // Invalid format
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullInput() {
        new ByteSize(null);
    }

    @Test
    public void testOriginalValue() {
        ByteSize size = new ByteSize("1.5GB");
        assertEquals("1.5GB", size.value());
    }

    @Test
    public void testTokenType() {
        ByteSize size = new ByteSize("1MB");
        assertEquals(TokenType.BYTE_SIZE, size.type());
    }
}
