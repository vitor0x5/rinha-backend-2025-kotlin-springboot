package com.vitor.gui.rinhakotlinspring

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import java.io.Serializable

@RestController
class Controller(
    private val restClient: RestClient,
    private val redsQueueTemplate: RedisTemplate<String, QueueItem>,
    private val redsPaymentItemTemplate: RedisTemplate<String, List<PaymentItem>>
) {
    companion object{
        private const val QUEUE_KEY: String = "PAYMENT_QUEUE"
        private const val PAYMENTS_KEY: String = "PAYMENT"
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPayment(
        @RequestBody input: InputBody
    ): String {
        val queueItem = QueueItem(
            amount = input.amount,
            correlationId = input.correlationId,
            requestedAt = System.currentTimeMillis()
        )
        redsQueueTemplate.opsForList().rightPush(QUEUE_KEY, queueItem)
        return ""
    }

    @GetMapping("/payments-summary")
    @ResponseStatus(HttpStatus.OK)
    fun getPayments(): OutputBody {
        var defaultPaymentsCount = 0L
        var fallbackPaymentsCount = 0L

        var defaultPaymentsAmount = 0.0
        var fallbackPaymentsAmount = 0.0

        val payments = redsPaymentItemTemplate.opsForValue().get(PAYMENTS_KEY)

        payments?.forEach {
            if(it.processor == 'D'){
                defaultPaymentsAmount += it.amount
                defaultPaymentsCount++
            } else {
                fallbackPaymentsAmount += it.amount
                fallbackPaymentsCount++
            }
        }
        return OutputBody(
            default = OutputValue(
                totalRequests = defaultPaymentsCount,
                totalAmount = defaultPaymentsAmount
            ),
            fallback = OutputValue(
                totalRequests = fallbackPaymentsCount,
                totalAmount = fallbackPaymentsAmount
            )
        )
    }
}


data class InputBody(
    val amount: Double,
    val correlationId: String,
)

data class OutputBody(
    val default: OutputValue,
    val fallback: OutputValue
)

data class OutputValue(
    val totalRequests:Long,
    val totalAmount:Double,
)



data class QueueItem(
    val amount: Double,
    val correlationId: String,
    val requestedAt: Long,
): Serializable