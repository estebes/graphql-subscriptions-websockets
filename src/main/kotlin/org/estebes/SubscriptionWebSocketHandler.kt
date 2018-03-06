package org.estebes

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.ExceptionWhileDataFetching
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.SubscriptionExecutionStrategy
import graphql.schema.GraphQLSchema
import org.apache.commons.lang3.exception.ExceptionUtils
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

/**
 * @author estebes
 */

class SubscriptionWebSocketHandler(
        val graphQLSchema: GraphQLSchema
) : TextWebSocketHandler() {
    override fun afterConnectionEstablished(session: WebSocketSession) {
        super.afterConnectionEstablished(session)
        println("Connected")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        println("WebSocket Closing")
        super.afterConnectionClosed(session, status)
        if (subscriptionRef.get() != null) subscriptionRef.get().cancel()
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        println("Message")
        val graphQL = GraphQL.newGraphQL(graphQLSchema).subscriptionExecutionStrategy(SubscriptionExecutionStrategy(dataFetcherExceptionHandler)).build()

        val lel = graphQL.execute("subscription Timer { timer { x } }")

        val stream = lel.getData<Publisher<ExecutionResult>>()

        stream.subscribe(object: Subscriber<ExecutionResult> {
            override fun onSubscribe(subscription: Subscription) {
                subscriptionRef.set(subscription)
                if (subscriptionRef.get() != null) subscriptionRef.get().request(1)
            }

            override fun onNext(result: ExecutionResult) {
                println("Subscription update")
                try {
                    session.sendMessage(TextMessage(ObjectMapper().writeValueAsString(result.toSpecification())))
                } catch (exception: IOException) {
                    exception.printStackTrace()
                }

                if (subscriptionRef.get() != null) subscriptionRef.get().request(1)
            }

            override fun onError(t: Throwable) {
                println("Subscription error")
                session.close(CloseStatus.SERVER_ERROR)
            }

            override fun onComplete() {
                println("Subscription complete")
                session.close(CloseStatus.NORMAL)
            }
        })
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        session.close(CloseStatus.SERVER_ERROR)
    }

    val dataFetcherExceptionHandler = DataFetcherExceptionHandler { handlerParameters ->
        val exception = ExceptionUtils.getRootCause(handlerParameters.exception)
        val sourceLocation = handlerParameters.field.sourceLocation
        val path = handlerParameters.path
        val error = ExceptionWhileDataFetching(path, exception, sourceLocation)
        handlerParameters.executionContext.addError(error, path)
    }

    private val subscriptionRef: AtomicReference<Subscription> = AtomicReference()
}