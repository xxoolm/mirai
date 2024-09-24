/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.console.terminal.net.mamoe.mirai.console.pure

import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.utils.DeprecatedSinceMirai

@Deprecated(
    message = "Please use MiraiConsoleTerminalLoader",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith(
        "MiraiConsoleTerminalLoader",
        "net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader"
    )
)
@DeprecatedSinceMirai(errorSince = "2.0", hiddenSince = "2.10")
object MiraiConsolePureLoader {
    @Deprecated(
        message = "for binary compatibility",
        level = DeprecationLevel.HIDDEN
    )
    @JvmStatic
    @DeprecatedSinceMirai(errorSince = "2.0", hiddenSince = "2.10")
    fun main(args: Array<String>) {
        System.err.println("WARNING: Mirai Console Pure已经更名为 Mirai Console Terminal")
        System.err.println("请使用新的入口点 net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader")
        MiraiConsoleTerminalLoader.main(args)
    }
}
