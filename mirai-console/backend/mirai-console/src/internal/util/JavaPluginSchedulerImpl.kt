/*
 * Copyright 2019-2022 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.console.internal.util

import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import net.mamoe.mirai.console.plugin.jvm.JavaPluginScheduler
import net.mamoe.mirai.utils.newCoroutineContextWithSupervisorJob
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.coroutines.CoroutineContext

internal class JavaPluginSchedulerImpl internal constructor(parentCoroutineContext: CoroutineContext) : CoroutineScope,
    JavaPluginScheduler {
    override val coroutineContext: CoroutineContext =
        parentCoroutineContext.newCoroutineContextWithSupervisorJob(this.toString())

    override fun repeating(intervalMs: Long, runnable: Runnable): Future<Void?> {
        return this.future {
            while (isActive) {
                runInterruptible(Dispatchers.IO) { runnable.run() }
                delay(intervalMs)
            }
            null
        }
    }

    override fun delayed(delayMillis: Long, runnable: Runnable): CompletableFuture<Void?> {
        return future {
            delay(delayMillis)
            runInterruptible(Dispatchers.IO) {
                runnable.run()
            }
            null
        }
    }

    override fun <R> delayed(delayMillis: Long, callable: Callable<R>): CompletableFuture<R> {
        return future {
            delay(delayMillis)
            runInterruptible(Dispatchers.IO) { callable.call() }
        }
    }

    override fun <R> async(supplier: Callable<R>): Future<R> {
        return future {
            runInterruptible(Dispatchers.IO) { supplier.call() }
        }
    }

    override fun async(runnable: Runnable): Future<Void?> {
        return future {
            runInterruptible(Dispatchers.IO) { runnable.run() }
            null
        }
    }
}