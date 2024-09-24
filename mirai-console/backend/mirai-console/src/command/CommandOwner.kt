/*
 * Copyright 2019-2023 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.console.command

import net.mamoe.mirai.console.compiler.common.ResolveContext
import net.mamoe.mirai.console.compiler.common.ResolveContext.Kind.PERMISSION_NAME
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionIdNamespace
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

/**
 * 指令的所有者. [JvmPlugin] 是一个 [CommandOwner].
 *
 * @see CommandManager.unregisterAllCommands 取消注册所有属于一个 [CommandOwner] 的指令
 * @see CommandManager.registeredCommands 获取已经注册了的属于一个 [CommandOwner] 的指令列表.
 */
public interface CommandOwner : PermissionIdNamespace {
    /**
     * 在构造指令时, [Command.permission] 默认会使用 [parentPermission] 作为 [Permission.parent]
     */
    public val parentPermission: Permission
}

/**
 * 代表控制台所有者. 所有的 mirai-console 内建的指令都属于 [ConsoleCommandOwner].
 *
 * 插件注册指令时不应该使用 [ConsoleCommandOwner].
 */
public object ConsoleCommandOwner : CommandOwner {
    @OptIn(ConsoleExperimentalApi::class)
    public override val parentPermission: Permission get() = BuiltInCommands.parentPermission

    public override fun permissionId(
        @ResolveContext(PERMISSION_NAME) name: String,
    ): PermissionId = PermissionId("console", name)
}