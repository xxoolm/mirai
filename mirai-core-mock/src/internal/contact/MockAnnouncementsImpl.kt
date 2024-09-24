/*
 * Copyright 2019-2023 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.mock.internal.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.announcement.Announcement
import net.mamoe.mirai.contact.announcement.AnnouncementImage
import net.mamoe.mirai.contact.announcement.OnlineAnnouncement
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.mock.contact.announcement.MockAnnouncements
import net.mamoe.mirai.mock.contact.announcement.MockOnlineAnnouncement
import net.mamoe.mirai.mock.contact.announcement.copy
import net.mamoe.mirai.mock.utils.mock
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.currentTimeSeconds
import net.mamoe.mirai.utils.withAutoClose
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

internal class MockAnnouncementsImpl(
    val group: Group,
) : MockAnnouncements {
    val announcements = ConcurrentHashMap<String, OnlineAnnouncement>()

    override fun asFlow(): Flow<OnlineAnnouncement> = announcements.values.asFlow()

    override fun asStream(): Stream<OnlineAnnouncement> = announcements.values.toList().stream()

    override suspend fun delete(fid: String): Boolean = announcements.remove(fid) != null

    override suspend fun get(fid: String): OnlineAnnouncement? = announcements[fid]

    @Suppress("MemberVisibilityCanBePrivate")
    internal fun putDirect(announcement: MockOnlineAnnouncement) {
        val ann = if (announcement.fid.isEmpty()) {
            announcement.copy(fid = UUID.randomUUID().toString())
        } else announcement
        if (ann.parameters.sendToNewMember) {
            announcements.entries.removeIf { (_, v) -> v.parameters.sendToNewMember }
        }
        announcements[ann.fid] = ann
        ann.group = group
    }

    override fun mockPublish(announcement: Announcement, actor: NormalMember, events: Boolean): OnlineAnnouncement {
        if (announcement.parameters.sendToNewMember) {
            announcements.elements().toList().firstOrNull { oa -> oa.parameters.sendToNewMember }
        }

        val ann = MockOnlineAnnouncement(
            content = announcement.content,
            parameters = announcement.parameters,
            senderId = actor.id,
            fid = UUID.randomUUID().toString(),
            allConfirmed = false,
            confirmedMembersCount = 0,
            publicationTime = currentTimeSeconds()
        )
        putDirect(ann)
        if (!events) return ann

        // TODO: mirai-core no other events about announcements
        return ann
    }

    override suspend fun publish(announcement: Announcement): OnlineAnnouncement {
        if (!group.botPermission.isOperator()) {
            throw PermissionDeniedException("Failed to publish a new announcement because bot don't have admin permission to perform it.")
        }
        return mockPublish(announcement, this.group.botAsMember, true)
    }

    override suspend fun uploadImage(resource: ExternalResource): AnnouncementImage = resource.withAutoClose {
        AnnouncementImage.create(group.bot.mock().uploadMockImage(resource).imageId, 500, 500)
    }

    override suspend fun members(fid: String, confirmed: Boolean): List<NormalMember> {
        if (!group.botPermission.isOperator()) {
            throw PermissionDeniedException("Only administrator have permission see announcement confirmed detail")
        }
        // TODO: 设置用户可读状态，而不返回全部
        return group.members.toList()
    }

    override suspend fun remind(fid: String) {
        if (!group.botPermission.isOperator()) {
            throw PermissionDeniedException("Only administrator have permission send announcement remind")
        }
    }
}
