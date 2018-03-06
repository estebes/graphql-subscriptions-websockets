package org.estebes

import graphql.schema.GraphQLSchema
import io.leangen.graphql.GraphQLSchemaGenerator
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

/**
 * @author estebes
 */

@Configuration
@EnableAutoConfiguration
@ComponentScan("org.estebes")
@SpringBootApplication
@EnableWebSocket
class Server(val subscriptionService: SubscriptionService) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketHandler(), "/subscriptions").setAllowedOrigins("*").withSockJS()
    }

    @Bean
    fun createWebSocketContainer(): ServletServerContainerFactoryBean {
        val container = ServletServerContainerFactoryBean()
        container.maxTextMessageBufferSize = 1024 * 1024
        container.maxBinaryMessageBufferSize = 1024 * 1024
        container.maxSessionIdleTimeout = 30 * 1000
        return container
    }

    @Bean
    fun webSocketHandler(): WebSocketHandler = PerConnectionWebSocketHandler(SubscriptionWebSocketHandler::class.java)

    @Bean
    fun schema(): GraphQLSchema = GraphQLSchemaGenerator()
            .withOperationsFromSingleton(subscriptionService)
            .generate()
}

fun main(args: Array<String>) {
    runApplication<Server>(*args)
}