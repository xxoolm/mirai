/*
 * Copyright 2019-2022 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package mirai.jar.before211

import net.mamoe.mirai.console.plugin.PluginManager
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import kotlin.test.assertFalse

/*
 * 2.11 以前打包的插件
 */
internal object Before211 : KotlinPlugin(
    JvmPluginDescription(
        id = "net.mamoe.tester.before211",
        version = "1.0.0",
        name = "Test Plugin",
    )
) {
    override fun onEnable() {
        assertFalse("Both before211 and after211 are loaded.") { PluginManager.plugins.any { it.id == "net.mamoe.tester.after211" } }
        throw AssertionError("Only net.mamoe.tester.before211 is loaded.")
    }
}
