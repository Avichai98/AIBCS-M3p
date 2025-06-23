package app.alertservice.queues

import app.alertservice.boundaries.AlertBoundary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaVehicleSender(
    private val kafkaTemplate: KafkaTemplate<String, AlertBoundary>
) {
    fun sendAlert(alert: AlertBoundary) {
        kafkaTemplate.send("alert-created", alert)
        println("📤 Sent AlertCreatedEvent to Kafka: $alert")
    }
}
