package com.vitor.gui.rinhakotlinspring

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.io.Serializable
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class Consumer(
    private val redisQueueTemplate: RedisTemplate<String, QueueItem>,
    private val redisPaymentsTemplate: RedisTemplate<String, List<PaymentItem>>,
    private val restClient: RestClient,
    private val paymentProcessorSelector: PaymentProcessorSelector,

    @Value("\${url.default}")
    private val defaultProcessorUrl: String,

    @Value("\${url.fallback}")
    private val fallbackProcessorUrl: String
) {
    companion object {
        private const val QUEUE_KEY = "PAYMENT_QUEUE"
        private const val PAYMENTS_KEY = "PAYMENT"
    }

    @Scheduled(fixedDelay = 500)
    fun consumeQueue() {
        val queueItem = redisQueueTemplate.opsForList().leftPop(QUEUE_KEY)
        if (queueItem != null) {
            val processor = processPayment(queueItem)
            savePayment(queueItem, processor)
        }
    }

    private fun processPayment(queueItem: QueueItem): Char {
        val url = if(paymentProcessorSelector.activeProcessor == 'D') {
            println("Using default payment processor")
            Pair("$defaultProcessorUrl/payments", 'D')
        } else {
            println("Using fallback payment processor")
            Pair("$fallbackProcessorUrl/payments",'F')
        }

        try {
            restClient.post()
                .uri { URI(url.first) }
                .body(ProcessorRequest(
                    amount = queueItem.amount,
                    correlationId = queueItem.correlationId,
                    requestedAt = Instant.ofEpochMilli(queueItem.requestedAt)
                        .atZone(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )).retrieve()
                .body(String::class.java)
        } catch (e: Exception) {
            redisQueueTemplate.opsForList().rightPush(QUEUE_KEY, queueItem)
            throw RuntimeException("Failed to process payment", e)
        }
        return url.second
    }

    private fun savePayment(queueItem: QueueItem, processor: Char) {
        val paymentItem = PaymentItem(
            amount = queueItem.amount,
            processor = processor,
            requestedAt = queueItem.requestedAt
        )
        val paymentsList = redisPaymentsTemplate.opsForValue().get(PAYMENTS_KEY) ?: emptyList()
        redisPaymentsTemplate.opsForValue().set(PAYMENTS_KEY, paymentsList.plus(paymentItem))
    }
}

data class ProcessorRequest(
    val amount: Double,
    val correlationId: String,
    val requestedAt: String
)

data class PaymentItem(
    val amount: Double,
    val processor: Char,
    val requestedAt: Long
): Serializable