package runtime.basic.worker_random

import konan.worker.*
import kotlin.collections.*
import kotlin.random.*
import kotlin.system.*
import kotlin.test.*

@Test
fun testRandomWorkers() {
    val seed = getTimeMillis()
    val workers = Array(5, { _ -> startWorker()})

    val attempts = 3
    val results = Array(attempts, { ArrayList<Int>() } )
    for (attempt in 0 until attempts) {
        // Produce a list of random numbers in each worker
        val futures = Array(workers.size, { workerIndex ->
            workers[workerIndex].schedule(TransferMode.CHECKED, { seed }) { seed ->
                Random.seed = seed
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

    results.forEach { list -> list.sort() }
    assertTrue(results[0] == results[1], "FAIL: got different sequences of generated values " +
            "first: ${results[0]}, second: ${results[1]}")

    workers.forEach {
        it.requestTermination().consume { _ -> }
    }
}