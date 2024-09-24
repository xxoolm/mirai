/*
 * Copyright 2019-2022 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.console.plugin.loader

import net.mamoe.mirai.console.plugin.Plugin
import net.mamoe.mirai.console.plugin.PluginManager
import net.mamoe.mirai.console.plugin.description.PluginDescription
import net.mamoe.mirai.console.plugin.jvm.JvmPluginLoader
import net.mamoe.mirai.utils.NotStableForInheritance
import java.io.File

/**
 * ['/plugins'][PluginManager.pluginsPath] 目录中的插件的加载器. 每个加载器需绑定一个后缀.
 *
 * @see AbstractFilePluginLoader 默认基础实现
 * @see JvmPluginLoader 内建的 Jar (JVM) 插件加载器.
 */
public interface FilePluginLoader<P : Plugin, D : PluginDescription> : PluginLoader<P, D> {
    /**
     * 所支持的插件文件后缀, 含 '.', 不区分大小写. 如 [JvmPluginLoader] 为 ".jar"
     */
    public val fileSuffix: String
}

/**
 * [FilePluginLoader] 的默认基础实现.
 *
 * @see FilePluginLoader
 */
public abstract class AbstractFilePluginLoader<P : Plugin, D : PluginDescription>(
    /**
     * 所支持的插件文件后缀, 含 '.', 不区分大小写. 如 [JvmPluginLoader] 为 ".jar"
     */
    public override val fileSuffix: String,
) : FilePluginLoader<P, D> {
    private fun pluginsFilesSequence(): Sequence<File> =
        PluginManager.pluginsFolder.listFiles().orEmpty().asSequence()
            .filter { it.isFile && it.name.endsWith(fileSuffix, ignoreCase = true) }

    /**
     * 读取扫描到的后缀与 [fileSuffix] 相同的文件中的插件实例, 但不 [加载][PluginLoader.load]
     */
    protected abstract fun Sequence<File>.extractPlugins(): List<P>

    @NotStableForInheritance // made non-final in 2.11
    public override fun listPlugins(): List<P> = pluginsFilesSequence().extractPlugins()
}