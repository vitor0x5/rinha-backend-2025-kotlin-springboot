package com.vitor.gui.rinhakotlinspring

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors


@Configuration
class Configurations {

    @Bean
    fun connectionFactory(): LettuceConnectionFactory {
        return LettuceConnectionFactory()
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory?): RedisTemplate<*, *> {
        val template: RedisTemplate<*, *> = RedisTemplate<Any, Any>()
        template.connectionFactory = connectionFactory!!

        return template
    }

    @Bean
    fun restClient(): RestClient {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1000))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .version(HttpClient.Version.HTTP_2)
            .build()

        return RestClient.builder()
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .build()
    }
}