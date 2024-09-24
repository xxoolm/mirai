/*
 * Copyright 2019-2023 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.utils.channels

public class ProducerFailureException(
    override val message: String? = "Producer failed to produce a value, see cause",
    override var cause: Throwable?
) : Exception() {
    private val unwrapped: Throwable by lazy {
        val cause = cause ?: return@lazy this
        this.cause = null
        cause.also { addSuppressed(this) }
    }

    public fun unwrap(): Throwable = unwrapped
}