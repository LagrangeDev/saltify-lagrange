package org.ntqqrev.saltify.lagrange.adapter.test

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.ntqqrev.saltify.api.context.event.message.MessageReceiveEvent
import org.ntqqrev.saltify.api.context.message.incoming.segment.ResourceLikeSegment
import org.ntqqrev.saltify.lagrange.adapter.LagrangeContextFactory
import org.ntqqrev.saltify.lagrange.adapter.LagrangeInit

private val logger = KotlinLogging.logger {  }

suspend fun main() {
    testEnv.scope.run {
        val ctx = LagrangeContextFactory.createContext(
            init = LagrangeInit(),
            env = testEnv,
            flow = MutableSharedFlow()
        )
        ctx.start()

        launch {
            ctx.channel.filterIsInstance<MessageReceiveEvent>()
                .collect {
                    logger.info { it.message }
                    it.message.segments.filterIsInstance<ResourceLikeSegment>()
                        .forEach { segment ->
                            println(ctx.getResourceTempUrl(segment.resourceId))
                        }
                }
        }

        delay(Long.MAX_VALUE)
    }
}