/*
 * Copyright 2019-2023 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.utils.channels

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Producer states.
 */
internal sealed interface ChannelState<T, V> {
    /*
     * 可变更状态的函数: [emit], [receiveOrNull], [expectMore], [finish], [finishExceptionally]
     * 
     * [emit] 和 [receiveOrNull] 为 suspend 函数, 在图中 "(suspend)" 表示挂起它们的协程, "(resume)" 表示恢复它们的协程.
     * 
     * "A ~~~~~~> B" 表示在切换为状态 A 后, 会挂起或恢复协程 B.
     * 
     * 
     * 
     *
     *                                  JustInitialized 
     *                                         |
     *                                         | 调用 [expectMore]
     *                                         |
     *                                         V                                                      
     *                                   ProducerReady (从此用户协程作为 producer 在后台运行)             
     *                                         |                                                      
     *                                         |                                                      
     *                                         |  <--------------------------------------------------
     *                                         |                                                     \
     *                                         V                                                      |
     *                                     Producing   ([expectMore] 结束)                             |
     *                                     |       \                                                  |
     *                        调用          |       \                                                  |
     *                    [receiveOrNull]  |        \ 调用 [emit]                                      |
     *                                     /         \                                                |
     *                                    /           \                                               |
     *                                   /             \                                              |
     *                                   |               \                                            |
     *                                   |                \                                           |
     *                                   |                 |-------------                             |
     *                                   |                 |             \                            |
     *                                   |                 |              |                           |
     *                                   |                  \             |                           |
     *                                   |                   \            |                           |
     *                                   |                    \           |                           | 
     *                                   |                     |          |                           |
     *                                   V       (resume)      V          |                           |            
     *              ([receiveOrNull] suspend) <~~~~~~~~~~~~ Consuming     |                           |                  
     *                                   |                     /          |                           |
     *                                   |                    /           |                           |
     *                                   |   /---------------/            |                           |
     *                                   |  / 调用 [receiveOrNull]         |                           |
     *                                   | /                              |                           |
     *                                   |/                               |                           |
     *                                   |                                |                           |
     *                                   |                                |                           |
     *                                   V                                |                           |  
     *         ([receiveOrNull] 结束)  Consumed                            |                           |
     *                                   |                                |                           |
     *                                   | 调用 [expectMore]               |                           |
     *                                   |                                |                           |
     *                                   V            (resume)            V                           |
     *                             ProducerReady ~~~~~~~~~~~~~~~~> ([emit] suspend)                   |
     *                                   |                                |                           |
     *                                   |                                |                           |
     *                                   |                                V                           |
     *                                   |                           ([emit] 结束)                     |
     *                                   |                                                            |
     *                                   |------------------------------------------------------------+
     *                                                             (返回顶部 Producing)
     * 
     * 
     * 
     * 在任意状态调用 [finish] 以及 [finishExceptionally], 可将状态转移到最终状态 [Finished].
     * 
     * 在一个状态中调用图中未说明的函数会抛出 [IllegalProducerStateException].
     */

    /**
     * Override this function to produce good debug information
     */
    abstract override fun toString(): String

    class JustInitialized<T, V> : ChannelState<T, V> {
        override fun toString(): String = "JustInitialized"
    }

    sealed interface HasProducer<T, V> : ChannelState<T, V> {
        val producer: OnDemandSendChannel<T, V>
    }

    // Producer is not running until `expectMore`. `emit` and `receiveOrNull` not allowed.
    class ProducerReady<T, V>(
        launchProducer: () -> OnDemandSendChannel<T, V>,
    ) : HasProducer<T, V> {
        // Lazily start the producer job since it's on-demand
        override val producer: OnDemandSendChannel<T, V> by lazy(launchProducer) // `lazy` is synchronized

        override fun toString(): String = "ProducerReady"
    }

    // Producer is running. `emit` and `receiveOrNull` both allowed.
    class Producing<T, V>(
        override val producer: OnDemandSendChannel<T, V>,
        parentJob: Job,
    ) : HasProducer<T, V> {
        val deferred: CompletableDeferred<V> by lazy { CompletableDeferred<V>(parentJob) }

        override fun toString(): String = "Producing(deferred.completed=${deferred.isCompleted})"
    }

    // Producer is suspended because it called `emit`. Expecting `receiveOrNull`.
    class Consuming<T, V>(
        override val producer: OnDemandSendChannel<T, V>,
        val value: Deferred<V>,
        parentCoroutineContext: CoroutineContext,
    ) : HasProducer<T, V> {
        val producerLatch: CompletableDeferred<T> = CompletableDeferred(parentCoroutineContext[Job])

        override fun toString(): String {
            @OptIn(ExperimentalCoroutinesApi::class)
            val completed =
                value.runCatching { getCompleted().toString() }.getOrNull() // getCompleted() is experimental
            return "Consuming(value=$completed)"
        }
    }

    // Producer is suspended. `expectMore` will resume producer with a ticket.
    class Consumed<T, V>(
        override val producer: OnDemandSendChannel<T, V>,
        val producerLatch: CompletableDeferred<T>
    ) : HasProducer<T, V> {
        override fun toString(): String = "Consumed($producerLatch)"
    }

    class Finished<T, V>(
        private val previousState: ChannelState<T, V>,
        val exception: Throwable?,
    ) : ChannelState<T, V> {
        val isSuccess: Boolean get() = exception == null

        fun createAlreadyFinishedException(cause: Throwable?): IllegalChannelStateException {
            val exception = exception
            val causeMessage = if (cause == null) {
                ""
            } else {
                ", but attempting to finish with the cause $cause"
            }
            return if (exception == null) {
                IllegalChannelStateException(
                    this,
                    "Producer has already finished normally$causeMessage. Previous state was: $previousState",
                    cause = cause
                )
            } else {
                IllegalChannelStateException(
                    this,
                    "Producer has already finished with the suppressed exception$causeMessage. Previous state was: $previousState",
                    cause = cause
                ).apply {
                    addSuppressed(exception)
                }
            }
        }

        override fun toString(): String = "Finished($previousState, $exception)"
    }
}