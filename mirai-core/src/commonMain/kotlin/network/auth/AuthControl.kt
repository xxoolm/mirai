/*
 * Copyright 2019-2023 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.internal.network.auth

import net.mamoe.mirai.auth.BotAuthInfo
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.internal.network.components.SsoProcessorImpl
import net.mamoe.mirai.internal.utils.asUtilsLogger
import net.mamoe.mirai.internal.utils.subLogger
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.channels.OnDemandChannel
import net.mamoe.mirai.utils.channels.OnDemandReceiveChannel
import net.mamoe.mirai.utils.channels.ProducerFailureException
import kotlin.coroutines.CoroutineContext


/**
 * Event sequence:
 *
 * 1. Starts a user coroutine [BotAuthorization.authorize].
 * 2. User coroutine
 */
internal class AuthControl(
    private val botAuthInfo: BotAuthInfo,
    private val authorization: BotAuthorization,
    private val logger: MiraiLogger,
    parentCoroutineContext: CoroutineContext,
) {
    internal val exceptionCollector = ExceptionCollector()

    private val userDecisions: OnDemandReceiveChannel<Throwable?, SsoProcessorImpl.AuthMethod> =
        OnDemandChannel(
            parentCoroutineContext,
            logger.subLogger("AuthControl/UserDecisions").withSwitch(DEBUG_LOGGING).asUtilsLogger()
        ) { _ ->
            val sessionImpl = SafeBotAuthSession(this)
            authorization.authorize(sessionImpl, botAuthInfo) // OnDemandChannel handles exceptions for us
        }

    fun start() {
        userDecisions.expectMore(null)
    }

    // Does not throw
    suspend fun acquireAuth(): SsoProcessorImpl.AuthMethod {
        logger.verbose { "[AuthControl/acquire] Acquiring auth method" }

        val rsp = try {
            userDecisions.receiveOrNull() ?: SsoProcessorImpl.AuthMethod.NotAvailable
        } catch (e: ProducerFailureException) {
            SsoProcessorImpl.AuthMethod.Error(e.unwrap())
        }

        logger.debug { "[AuthControl/acquire] Authorization responded: $rsp" }
        return rsp
    }

    fun actMethodFailed(cause: Throwable) {
        logger.verbose { "[AuthControl/resume] Fire auth failed with cause: $cause" }
        userDecisions.expectMore(cause)
    }

    fun actComplete() {
        logger.verbose { "[AuthControl/resume] Fire auth completed" }
        userDecisions.close()
    }

    private companion object {
        private val DEBUG_LOGGING = systemProp("mirai.network.auth.logging", false)
    }
}
