/*
 * Copyright 2019-2023 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_SUPER_CLASS")
@file:OptIn(ConsoleExperimentalApi::class)

package net.mamoe.mirai.console.internal.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.mamoe.mirai.console.data.AbstractPluginData
import net.mamoe.mirai.console.data.AbstractPluginData.ValueNode
import net.mamoe.mirai.console.data.PluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.yamlkt.Comment
import net.mamoe.yamlkt.YamlNullableDynamicSerializer
import java.lang.reflect.Constructor
import kotlin.reflect.KAnnotatedElement

/**
 * Internal implementation for [PluginData] including:
 * - Reflection on Kotlin properties and Java fields
 * - Auto-saving
 */
internal abstract class PluginDataImpl {
    init {
        @Suppress("LeakingThis")
        check(this is AbstractPluginData)
    }

    private fun findNodeInstance(name: String): ValueNode<*>? {
        check(this is AbstractPluginData)
        return valueNodes.firstOrNull { it.valueName == name }
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal open val updaterSerializer: KSerializer<Unit> = object : KSerializer<Unit> {
        override val descriptor: SerialDescriptor by lazy {
            check(this@PluginDataImpl is AbstractPluginData)
            kotlinx.serialization.descriptors.buildClassSerialDescriptor((this@PluginDataImpl as PluginData).saveName) {
                for (valueNode in valueNodes) valueNode.run {
                    element(valueName, updaterSerializer.descriptor, annotations = annotations, isOptional = true)
                }
            }
        }

        override fun deserialize(decoder: Decoder) {
            val descriptor = descriptor
            with(decoder.beginStructure(descriptor)) {
                if (runCatching { decodeSequentially() }.getOrElse { false }) {
                    var index = 0
                    repeat(decodeCollectionSize(descriptor)) {
                        val valueName = decodeSerializableElement(descriptor, index++, String.serializer())
                        val node = findNodeInstance(valueName)
                        if (node == null) {
                            decodeSerializableElement(descriptor, index++, YamlNullableDynamicSerializer)
                        } else {
                            decodeSerializableElement(descriptor, index++, node.updaterSerializer)
                        }
                    }
                } else {
                    outerLoop@ while (true) {
                        innerLoop@ while (true) {
                            val index = decodeElementIndex(descriptor)
                            if (index == CompositeDecoder.DECODE_DONE) {
                                //check(valueName == null) { "name must be null at this moment." }
                                break@outerLoop
                            }

                            val node = findNodeInstance(descriptor.getElementName(index))
                            if (node == null) {
                                decodeSerializableElement(descriptor, index, YamlNullableDynamicSerializer)
                            } else {
                                decodeSerializableElement(descriptor, index, node.updaterSerializer)
                            }


                            break@innerLoop
                        }

                    }
                }
                endStructure(descriptor)
            }
        }

        override fun serialize(encoder: Encoder, value: Unit) {
            check(this@PluginDataImpl is AbstractPluginData)

            val descriptor = descriptor
            with(encoder.beginStructure(descriptor)) {
                repeat(descriptor.elementsCount) { index ->
                    encodeSerializableElement(
                        descriptor,
                        index,
                        valueNodes.find { it.valueName == descriptor.getElementName(index) }?.updaterSerializer
                            ?: error("Cannot find a serializer for ${descriptor.getElementName(index)}"),
                        Unit
                    )
                }
                endStructure(descriptor)
            }
        }

    }
}

internal fun KAnnotatedElement.getAnnotationListForValueSerialization(): List<Annotation> {
    return this.annotations.mapNotNull {
        when (it) {
            is SerialName -> error("@SerialName is not supported on Value. Please use @ValueName instead")
            is ValueName -> null
            is ValueDescription -> COMMENT_CONSTRUCTOR(it.value)
            else -> it
        }
    }
}


private val COMMENT_CONSTRUCTOR = findAnnotationImplementationClassConstructor<Comment>()!!

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <T : Any?> Constructor<T>.invoke(vararg args: Any?): T = this.newInstance(*args)

internal inline fun <reified T : Any> findAnnotationImplementationClassConstructor(): Constructor<out T>? {
    @Suppress("UNCHECKED_CAST")
    return T::class.nestedClasses
        .find { it.simpleName?.endsWith("Impl") == true }?.java?.run {
            constructors.singleOrNull()
        } as Constructor<out T>?
}