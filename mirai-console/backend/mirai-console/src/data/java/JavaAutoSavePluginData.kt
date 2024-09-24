/*
 * Copyright 2019-2023 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

@file:Suppress("unused", "EXPOSED_SUPER_CLASS")
@file:OptIn(ConsoleExperimentalApi::class)

package net.mamoe.mirai.console.data.java

import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.internal.data.cast
import net.mamoe.mirai.console.internal.data.setValueBySerializer
import net.mamoe.mirai.console.internal.data.valueImpl
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.JavaFriendlyApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType

/**
 * 供 Java 用户使用的 [PluginData]. 参考 [PluginData] 以获取更多信息.
 *
 * 仍然更推荐在项目中混用 Java 和 Kotlin, 使用 Kotlin 编写 [PluginData].
 * 也可以使用其他持久化库替代 [PluginData].
 *
 * 在 [JvmPlugin] 的典型实现方式:
 * ```
 * // PluginMain.java
 * public final class PluginMain extends JavaPlugin {
 *     public static PluginMain INSTANCE;
 *     public PluginMain() {
 *          INSTANCE = this;
 *     }
 *     @Override
 *     public onLoad() {
 *          this.reloadPluginData(MyPluginData.INSTANCE); // 读取文件等
 *     }
 * }
 *
 * // MyPluginData.java
 * public class MyPluginData extends JavaAutoSavePluginData {
 *     public static final MyPluginData INSTANCE = new MyPluginData();
 *
 *     public final Value<String> string = value("test"); // 默认值 "test"
 *
 *     public final Value<List<String>> list = typedValue(createKType(List.class, createKType(String.class))); // 无默认值, 自动创建空 List
 *
 *     public final Value<Map<Long, Object>> custom = typedValue(
 *             createKType(Map.class, createKType(Long.class), createKType(Object.class)),
 *             new HashMap<Long, Object>() {{ // 带默认值
 *                 put(123L, "ok");
 *             }}
 *     );
 * }
 * ```
 *
 * 使用时, 需要使用 `.get()`, 如:
 * ```
 * Value<List<String>> theList = MyPluginData.INSTANCE.list; // 获取 Value 实例. Value 代表一个追踪自动保存的值.
 * List<String> actualList = theList.get();
 * theList.set();
 * ```
 *
 * **注意**: 由于实现特殊, 请不要在初始化 Value 时就使用 `.get()`. 这可能会导致自动保存追踪失效. 必须在使用时才调用 `.get()` 获取真实数据对象.
 *
 * @see PluginData
 * @since 2.11
 */
@JavaFriendlyApi
public abstract class JavaAutoSavePluginData public constructor(saveName: String) : AutoSavePluginData(saveName),
    PluginConfig {

    //// region JavaAutoSavePluginData_value_primitives CODEGEN ////

    /**
     * 创建一个名称为 [name], 类型为 [Byte] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: Byte): SerializerAwareValue<Byte> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    /**
     * 创建一个名称为 [name], 类型为 [Short] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: Short): SerializerAwareValue<Short> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    /**
     * 创建一个名称为 [name], 类型为 [Int] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: Int): SerializerAwareValue<Int> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    /**
     * 创建一个名称为 [name], 类型为 [Long] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: Long): SerializerAwareValue<Long> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    /**
     * 创建一个名称为 [name], 类型为 [Float] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: Float): SerializerAwareValue<Float> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    /**
     * 创建一个名称为 [name], 类型为 [Double] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: Double): SerializerAwareValue<Double> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    /**
     * 创建一个名称为 [name], 类型为 [Char] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: Char): SerializerAwareValue<Char> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    /**
     * 创建一个名称为 [name], 类型为 [Boolean] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: Boolean): SerializerAwareValue<Boolean> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    /**
     * 创建一个名称为 [name], 类型为 [String] 的 [Value], 并设置初始值为 [default].
     */
    public fun value(name: String, default: String): SerializerAwareValue<String> =
        valueImpl(default).apply { track(this, name, emptyList()) }

    //// endregion JavaAutoSavePluginData_value_primitives CODEGEN ////

    /**
     * 创建一个支持泛型的 [Value].
     *
     * 对于 [Map], [Set], [List], [ConcurrentMap] 等标准库类型, 这个函数会尝试构造 [LinkedHashMap], [LinkedHashSet], [ArrayList], [ConcurrentHashMap] 等相关类型.
     * 而对于自定义数据类型, 本函数只会反射获取 [objectInstance][KClass.objectInstance] 或使用*无参构造器*构造实例.
     *
     * @param type Kotlin 类型. 可通过 [createKType] 获得
     *
     * @param T 类型 T. 仅支持:
     * - 基础数据类型, [String]
     * - 标准库集合类型 ([List], [Map], [Set], [ConcurrentMap])
     * - 标准库数据类型 ([Map.Entry], [Pair], [Triple])
     * - 使用 [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) 的 [Serializable] 标记的 **Kotlin** 类
     */
    @JvmOverloads
    public fun <T : Any> typedValue(name: String, type: KType, default: T? = null): SerializerAwareValue<T> {
        val value = valueImpl<T>(type, type.classifier!!.cast())
        if (default != null) value.setValueBySerializer(default)
        track(value, name, emptyList())
        return value
    }

    /**
     * @since 2.11
     */
    @JavaFriendlyApi
    public companion object {
        /**
         * 根据 [Class] 及泛型参数获得一个类型
         *
         * 如要获得一个 `Map<String, Long>` 的类型,
         * ```java
         * KType type = JPluginDataHelper.createKType(Map.java, createKType(String.java), createKType(Long.java))
         * ```
         *
         * @param genericArguments 带有顺序的泛型参数
         */
        @JvmStatic
        public fun <T : Any> createKType(clazz: Class<T>, nullable: Boolean, vararg genericArguments: KType): KType {
            return clazz.kotlin.createType(genericArguments.map { KTypeProjection(KVariance.INVARIANT, it) }, nullable)
        }

        /**
         * 根据 [Class] 及泛型参数获得一个不可为 `null` 的类型
         *
         * @see createKType
         */
        @JvmStatic
        public fun <T : Any> createKType(clazz: Class<T>, vararg genericArguments: KType): KType {
            return createKType(clazz, false, *genericArguments)
        }
    }
}