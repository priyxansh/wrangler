/*
 * Copyright © 2017-2025 Cask Data, Inc.
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

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ByteSizeTest {

    @Test
    public void testByteSizeParsing() {
        assertEquals(10240L, new ByteSize("10kb").getBytes()); // 10 KB = 10 * 1024
        assertEquals(1572864L, new ByteSize("1.5MB").getBytes(), 0.001); // 1.5 MB = 1.5 * 1024 * 1024
        assertEquals(1024L, new ByteSize("1KB").getBytes()); // 1 KB = 1024
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteSizeInvalidFormat() {
        new ByteSize("abc"); // Should throw IllegalArgumentException
    }
}
