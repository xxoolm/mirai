/*
 * Copyright 2019-2022 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.contact.active

import net.mamoe.mirai.contact.NormalMember

/**
 * 活跃排行榜记录
 * @property memberName 发言者名称
 * @property memberId 发言者 ID
 * @property member 发言者的群员实例
 * @property temperature 活跃度
 * @property score 活跃积分
 * @since 2.13
 */
public class ActiveRankRecord internal constructor(
    public val memberName: String,
    public val memberId: Long,
    public val member: NormalMember?,
    public val temperature: Int,
    public val score: Int
)