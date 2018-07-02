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
    abstract fun nextInt(bound: Int): Int

    abstract fun nextLong(): Long

    /**
     * A default pseudo-random generator that relies on POSIX's random/srandom.
     * This implementation is a singleton generator. A sequence of generated numbers
     * and seed is shared between all workers and native threads.
     */
    companion object : Random() {
        private val _seed = AtomicInt(getTimeNanos().toInt())
        init { updateSeed() }

        /**
         * Random generator seed value.
         */
        var seed: Int
            get() = _seed.get()
            set(value) {
                _seed.compareAndSwap(_seed.get(), value)
                updateSeed()
            }
        private inline fun updateSeed() = srandom(_seed.get())

        /**
         * Returns a pseudo-random Int number.
         */
        override fun nextInt(): Int = random()

        /**
         * Returns a pseudo-random Int value between 0 and specified value (exclusive)
         */
        override fun nextInt(bound: Int): Int {
            if (bound <= 0) throw IllegalArgumentException("Incorrect bound: $bound")

            // As there are no guarantee for the rand on some platforms
            // about low-order bits distribution use the same technique as Java does
            // throwing away low-order bits
            if (bound and (bound - 1) == 0) {
                return ((bound * random().toLong()) shr 31).toInt();
            }

            var m: Int
            do {
                var r = random()
                m = r % bound
            } while (r - m + (bound - 1) < 0)
            return m
        }

        /**
         * Returns a pseudo-random Long number.
         */
        override fun nextLong(): Long = (random().toLong() shl 32) + random().toLong()
    }
}