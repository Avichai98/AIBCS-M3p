package app.dataservice.queues

import app.dataservice.boundaries.VehicleBoundary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaVehicleSender(
    private val kafkaTemplate: KafkaTemplate<String, VehicleBoundary>
) {
    fun sendUpdatedVehicleState(vehicle : VehicleBoundary) {
        println("Before sending vehicle to Kafka: $vehicle")
        kafkaTemplate.send("vehicle-state-updated", vehicle)
        println("📤 Sent Updated vehicle to Kafka: $vehicle")
    }
}
