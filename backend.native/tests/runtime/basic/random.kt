package runtime.basic.random

import konan.worker.*
import kotlin.collections.*
import kotlin.random.*
import kotlin.system.*
import kotlin.test.*

/**
 * Tests that setting the same seed make random generate the same sequence
 */
private inline fun <reified T> testReproducibility(seed: Int, generator: () -> T) {
    // Reset seed. This will make Random to start a new sequence
    Random.seed = seed
    val first = Array<T>(50, { i -> generator() }).toList()

    // Reset seed and try again
    Random.seed = seed
    val second = Array<T>(50, { i -> generator() }).toList()
    assertTrue(first == second, "FAIL: got different sequences of generated values " +
            "first: $first, second: $second")
}

/**
 * Tests that setting seed makes random generate different sequence.
 */
private inline fun <reified T> testDifference(generator: () -> T) {
    Random.seed = 12345678
    val first = Array<T>(100, { i -> generator() }).toList()

    Random.seed = 87654321
    val second = Array<T>(100, { i -> generator() }).toList()
    assertTrue(first != second, "FAIL: got the same sequence of generated values " +
            "first: $first, second: $second")
}

@Test
fun testInts() {
    testReproducibility(getTimeMillis().toInt(), { Random.nextInt() })
    testReproducibility(Int.MAX_VALUE, { Random.nextInt() })
}

@Test
fun testLong() {
    testReproducibility(getTimeMillis().toInt(), { Random.nextLong() })
    testReproducibility(Int.MAX_VALUE, { Random.nextLong() })
}

@Test
fun testDiffInt() = testDifference { Random.nextInt() }

@Test
fun testDiffLong() = testDifference { Random.nextLong() }

@Test
fun testRandomWorkers() {
    val seed = getTimeMillis().toInt()
    val workers = Array(5, { _ -> startWorker()})

    val attempts = 3
    val results = Array(attempts, { ArrayList<Int>() } )
    for (attempt in 0 until attempts) {
        Random.seed = seed
        // Produce a list of random numbers in each worker
        val futures = Array(workers.size, { workerIndex ->
            workers[workerIndex].schedule(TransferMode.CHECKED, { workerIndex }) { input ->
                Array(50, { Random.nextInt() }).toList()
            }
        })
        // Now collect all results into current attempt's list
        val futureSet = futures.toSet()
        for (i in 0 until futureSet.size) {
            val ready = futureSet.waitForMultipleFutures(10000)
            ready.forEach { results[attempt].addAll(it.result()) }
        }
    }

// TODO: uncomment when there are stable synchronized/thread-local random available
//    results.forEach { list -> list.sort() }
//    assertTrue(results[0] == results[1], "FAIL: got different sequences of generated values " +
//            "first: ${results[0]}, second: ${results[1]}")

    workers.forEach {
        it.requestTermination().consume { _ -> }
    }
}