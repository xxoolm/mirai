/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.internal.contact.roaming

import kotlinx.coroutines.flow.Flow
import net.mamoe.mirai.contact.roaming.RoamingMessageFilter
import net.mamoe.mirai.contact.roaming.RoamingMessages
import net.mamoe.mirai.contact.roaming.RoamingSupported
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.utils.JavaFriendlyAPI
import java.util.stream.Stream

internal class MockRoamingMessages(
    internal val contact: RoamingSupported,
) : RoamingMessages {
    override suspend fun getMessagesIn(
        timeStart: Long,
        timeEnd: Long,
        filter: RoamingMessageFilter?
    ): Flow<MessageChain> {
        TODO("Not yet implemented")
    }

    @JavaFriendlyAPI
    override suspend fun getMessagesStream(
        timeStart: Long,
        timeEnd: Long,
        filter: RoamingMessageFilter?
    ): Stream<MessageChain> {
        TODO()
    }
}
