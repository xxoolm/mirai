/*
 * Copyright 2019-2022 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.internal.network.notice

import kotlinx.coroutines.cancel
import net.mamoe.mirai.Mirai
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.internal.QQAndroidBot
import net.mamoe.mirai.internal.contact.FriendImpl
import net.mamoe.mirai.internal.contact.GroupImpl
import net.mamoe.mirai.internal.contact.StrangerImpl
import net.mamoe.mirai.internal.contact.impl
import net.mamoe.mirai.internal.contact.info.FriendInfoImpl
import net.mamoe.mirai.internal.contact.info.GroupInfoImpl
import net.mamoe.mirai.internal.contact.info.MemberInfoImpl
import net.mamoe.mirai.internal.contact.info.StrangerInfoImpl
import net.mamoe.mirai.internal.getGroupByUin
import net.mamoe.mirai.internal.network.protocol.data.jce.StGroupRankInfo
import net.mamoe.mirai.internal.network.protocol.data.jce.StTroopNum
import net.mamoe.mirai.internal.network.protocol.data.proto.MsgComm
import net.mamoe.mirai.internal.network.protocol.packet.list.FriendList

internal interface NewContactSupport { // can be a marker interface when context receivers are available.

    fun MsgComm.Msg.getNewMemberInfo(): MemberInfoImpl {
        return MemberInfoImpl(
            nameCard = msgHead.authNick.ifEmpty { msgHead.fromNick },
            permission = MemberPermission.MEMBER,
            specialTitle = "",
            muteTimestamp = 0,
            uin = msgHead.authUin,
            nick = msgHead.authNick.ifEmpty { msgHead.fromNick },
            remark = "",
            anonymousId = null,
        )
    }

    suspend fun QQAndroidBot.addNewGroupByCode(code: Long): GroupImpl? {
        if (getGroup(code) != null) return null
        return getNewGroup(code)?.apply { groups.delegate.add(this) }
    }

    // final
    suspend fun QQAndroidBot.addNewGroupByUin(groupUin: Long): GroupImpl? {
        if (getGroupByUin(groupUin) != null) return null
        return addNewGroupByCode(Mirai.calculateGroupCodeByGroupUin(groupUin))
    }

    suspend fun QQAndroidBot.addNewGroup(stTroopNum: StTroopNum, stGroupRankInfo: StGroupRankInfo?): GroupImpl? {
        if (getGroup(stTroopNum.groupCode) != null) return null
        return getNewGroup(stTroopNum, stGroupRankInfo).apply { groups.delegate.add(this) }
    }

    fun QQAndroidBot.removeStranger(id: Long): StrangerImpl? {
        val instance = strangers[id] ?: return null
        strangers.remove(instance.id)
        instance.cancel()
        return instance
    }

    fun QQAndroidBot.removeFriend(id: Long): FriendImpl? {
        val instance = friends[id] ?: return null
        friends.remove(instance.id)
        instance.cancel()
        return instance
    }

    fun QQAndroidBot.addNewFriendAndRemoveStranger(info: FriendInfoImpl): FriendImpl? {
        if (friends.contains(info.uin)) return null
        strangers[info.uin]?.let { removeStranger(it.id) }
        val friend = Mirai.newFriend(bot, info).impl()
        friends.delegate.add(friend)
        return friend
    }

    fun QQAndroidBot.addNewStranger(info: StrangerInfoImpl): StrangerImpl? {
        if (friends.contains(info.uin)) return null // cannot have both stranger and friend
        if (strangers.contains(info.uin)) return null
        val stranger = Mirai.newStranger(bot, info).impl()
        strangers.delegate.add(stranger)
        return stranger
    }

    private suspend fun QQAndroidBot.getNewGroup(groupCode: Long): GroupImpl? {
        val response = network.sendAndExpect(
            FriendList.GetTroopListSimplify(client),
            timeout = 10_000, attempts = 5
        )

        val troopNum = response.groups.firstOrNull { it.groupCode == groupCode } ?: return null
        val groupRankInfo = response.ranks.find { it.dwGroupCode == groupCode }

        return getNewGroup(troopNum, groupRankInfo)
    }

    private suspend fun QQAndroidBot.getNewGroup(troopNum: StTroopNum, stGroupRankInfo: StGroupRankInfo?): GroupImpl {
        return GroupImpl(
            bot = this,
            parentCoroutineContext = coroutineContext,
            id = troopNum.groupCode,
            groupInfo = GroupInfoImpl(troopNum, stGroupRankInfo),
            members = Mirai.getRawGroupMemberList(
                this,
                troopNum.groupUin,
                troopNum.groupCode,
                troopNum.dwGroupOwnerUin,
            ),
        )
    }

}