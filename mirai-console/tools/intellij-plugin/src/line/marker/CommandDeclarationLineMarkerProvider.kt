/*
 * Copyright 2019-2022 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.console.intellij.line.marker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import net.mamoe.mirai.console.intellij.assets.Icons
import net.mamoe.mirai.console.intellij.resolve.getElementForLineMark
import net.mamoe.mirai.console.intellij.resolve.isSimpleCommandHandlerOrCompositeCommandSubCommand
import net.mamoe.mirai.console.intellij.util.runIgnoringErrors
import org.jetbrains.kotlin.psi.KtNamedFunction

class CommandDeclarationLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        when (element) {
            is KtNamedFunction -> {
                if (!element.isSimpleCommandHandlerOrCompositeCommandSubCommand()) return null

                runIgnoringErrors { // not showing icons is better than throwing exception every time doing inspection
                    return Info(getElementForLineMark(element.funKeyword ?: element.identifyingElement ?: element))
                }
            }
            is PsiMethod -> {
                if (!element.isSimpleCommandHandlerOrCompositeCommandSubCommand()) return null

                runIgnoringErrors { // not showing icons is better than throwing exception every time doing inspection
                    return Info(getElementForLineMark(element.identifyingElement ?: element))
                }
            }
            else -> return null
        }

    }

    class Info(callElement: PsiElement) : LineMarkerInfo<PsiElement>(
        callElement,
        callElement.textRange,
        Icons.CommandDeclaration,
        { "子指令定义" },
        null,
        GutterIconRenderer.Alignment.RIGHT,
        { "子指令定义" }
    )
}