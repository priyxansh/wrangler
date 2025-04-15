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

public class TimeDurationTest {

    @Test
    public void testTimeDurationParsing() {
        assertEquals(5000000L, new TimeDuration("5ms").getNanos()); // 5 ms = 5 * 1,000,000
        assertEquals(2100000000L, new TimeDuration("2.1s").getNanos(), 0.001); // 2.1 s = 2.1 * 1,000,000,000
        assertEquals(1000000L, new TimeDuration("1ms").getNanos()); // 1 ms = 1,000,000
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTimeDurationInvalidFormat() {
        new TimeDuration("xyz"); // Should throw IllegalArgumentException
    }
}
