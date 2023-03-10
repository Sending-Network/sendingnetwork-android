/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.sdn.android.sdk.internal.util

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.sdn.android.sdk.SDNTest

@FixMethodOrder(MethodSorters.JVM)
class MathUtilTest : SDNTest {

    @Test
    fun testComputeBestChunkSize0() = doTest(0, 100, 1, 0)

    @Test
    fun testComputeBestChunkSize1to99() {
        for (i in 1..99) {
            doTest(i, 100, 1, i)
        }
    }

    @Test
    fun testComputeBestChunkSize100() = doTest(100, 100, 1, 100)

    @Test
    fun testComputeBestChunkSize101() = doTest(101, 100, 2, 51)

    @Test
    fun testComputeBestChunkSize199() = doTest(199, 100, 2, 100)

    @Test
    fun testComputeBestChunkSize200() = doTest(200, 100, 2, 100)

    @Test
    fun testComputeBestChunkSize201() = doTest(201, 100, 3, 67)

    @Test
    fun testComputeBestChunkSize240() = doTest(240, 100, 3, 80)

    private fun doTest(listSize: Int, limit: Int, expectedNumberOfChunks: Int, expectedChunkSize: Int) {
        val result = computeBestChunkSize(listSize, limit)

        result.numberOfChunks shouldBeEqualTo expectedNumberOfChunks
        result.chunkSize shouldBeEqualTo expectedChunkSize

        // Test that the result make sense, when we use chunked()
        if (result.chunkSize > 0) {
            generateSequence { "a" }
                    .take(listSize)
                    .chunked(result.chunkSize)
                    .shouldHaveSize(result.numberOfChunks)
        }
    }
}
