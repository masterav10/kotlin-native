/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlin.random

import konan.internal.*
import konan.worker.AtomicInt
import kotlin.system.getTimeNanos

abstract class Random {
    abstract fun nextInt(): Int

    abstract fun nextLong(): Long

    /**
     * A default pseudo-random generator that relies on POSIX's random/srandom.
     * This implementation is a singleton generator. A sequence of generated numbers
     * and seed is shared between all workers and native threads.
     */
    companion object : Random() {
        init { updateSeed() }

        /**
         * Random generator seed value.
         */
        var seed: Int = getTimeNanos().toInt()
            set(value) {
                field = value
                updateSeed()
            }
        private inline fun updateSeed() = srandom(seed)

        /**
         * Returns a pseudo-random Int number.
         */
        override fun nextInt(): Int = random()

        /**
         * Returns a pseudo-random Long number.
         */
        override fun nextLong(): Long = (random().toLong() shl 32) + random().toLong()
    }
}