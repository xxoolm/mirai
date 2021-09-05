/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.contact

import kotlinx.coroutines.cancel
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.BotLeaveEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.mock.MockBotDSL
import java.util.concurrent.CancellationException

@JvmBlockingBridge
public interface MockNormalMember : NormalMember, MockMember {
    // Mock api, no event broadcast
    override var lastSpeakTimestamp: Int
    override var joinTimestamp: Int
    override var muteTimeRemaining: Int

    /**
     * 广播该成员加入了群
     */
    @MockBotDSL
    public suspend fun broadcastMemberJoinEvent() {
        broadcastMemberJoinEvent(null)
    }

    /**
     * 广播该成员加入了群
     *
     * @param invitor 邀请者, 当邀请者不为 `null` 时广播 [MemberJoinEvent.Invite]
     */
    @MockBotDSL
    public suspend fun broadcastMemberJoinEvent(invitor: NormalMember?) {
        if (invitor == null) {
            MemberJoinEvent.Active(this)
        } else {
            MemberJoinEvent.Invite(this, invitor)
        }.broadcast()
    }

    /**
     * 广播该群员主动离开了群
     */
    @MockBotDSL
    public suspend fun broadcastMemberLeave() {
        if (group.members.delegate.remove(this)) {
            MemberLeaveEvent.Quit(this).broadcast()
            cancel(CancellationException("Member $id left"))
        }
    }

    /**
     * 广播该群员将 [bot] 踢出了群聊, 并同时在 bot 的群聊列表里删除该群
     */
    @MockBotDSL
    public suspend fun broadcastKickBot() {
        if (bot.groups.delegate.remove(group)) {
            BotLeaveEvent.Kick(this).broadcast()
            cancel(CancellationException("Bot was kicked"))
        }
    }
}