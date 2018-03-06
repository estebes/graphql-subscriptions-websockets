package org.estebes

import io.leangen.graphql.annotations.GraphQLSubscription
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import java.time.Duration
import java.time.LocalDateTime
import java.util.function.Consumer

/**
 * @author estebes
 */

@Service
class SubscriptionService {
    @GraphQLSubscription
    fun timer(): Publisher<Tick> {
        return Flux.generate(
                Consumer<SynchronousSink<LocalDateTime>> { sink ->
                    sink.next(LocalDateTime.now())
                })
                .map { Tick(it) }
                .delayElements(Duration.ofSeconds(1))
    }
}

data class Tick(val x: LocalDateTime)