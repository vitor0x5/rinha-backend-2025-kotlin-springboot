package com.vitor.gui.rinhakotlinspring

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import kotlin.compareTo
import kotlin.text.compareTo
import kotlin.times

@Component
class PaymentProcessorSelector(
    private val restClient: RestClient,
    private val redisTemplate: RedisTemplate<String, Char>,

    @Value("\${url.health-check}")
    private val defaultHealthCheckUrl: String?,

    @Value("\${url.fallback-payment-processor}")
    private val fallbackHealthCheckUrl: String?
) {
    companion object{
        private const val ACTIVE_PROCESSOR_KEY = "ACTIVE_PROCESSOR"
    }

    var activeProcessor: Char? = null

    @Scheduled(fixedDelay = 5000)
    fun execute() {
        if(defaultHealthCheckUrl.isNullOrBlank()) {
            activeProcessor = redisTemplate.opsForValue().get(ACTIVE_PROCESSOR_KEY)?: 'D'
            println("Active Processor $activeProcessor")
            return
        }

        val defaultProcessorResponse = checkProcessor("$defaultHealthCheckUrl/payments/service-health")

        activeProcessor = when {
            defaultProcessorResponse.failing -> {
                val fallbackProcessorResponse = checkProcessor("$fallbackHealthCheckUrl/payments/service-health")
                if (!fallbackProcessorResponse.failing) 'F' else 'D'
            }
            defaultProcessorResponse.minResponseTime > 1000 -> {
                val fallbackProcessorResponse = checkProcessor("$fallbackHealthCheckUrl/payments/service-health")
                when {
                    fallbackProcessorResponse.failing -> 'D'
                    fallbackProcessorResponse.minResponseTime < defaultProcessorResponse.minResponseTime * 0.5 -> 'F'
                    else -> 'D'
                }
            }
            else -> 'D'
        }
        println("Active Processor $activeProcessor")
        redisTemplate.opsForValue().set(ACTIVE_PROCESSOR_KEY, activeProcessor!!)
    }

    private fun checkProcessor(url: String): HealthResponse {
        return restClient.get().uri(url)
            .retrieve()
            .toEntity(HealthResponse::class.java)
            .body!!
    }
}

data class HealthResponse(
    val failing: Boolean,
    val minResponseTime: Long
)