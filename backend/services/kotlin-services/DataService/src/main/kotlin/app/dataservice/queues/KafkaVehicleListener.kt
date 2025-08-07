package app.dataservice.queues

import app.dataservice.boundaries.AlertBoundary
import app.dataservice.boundaries.VehicleBoundary
import app.dataservice.interfaces.AlertService
import app.dataservice.interfaces.VehicleService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class KafkaVehicleListener(
    private val kafkaVehicleSender: KafkaVehicleSender,
    private val vehicleService: VehicleService,
    private val alertService: AlertService
) {
    @KafkaListener(topics = ["vehicle-create"], groupId = "data-service")
    fun createVehicle(vehicle: VehicleBoundary) {
        println("📥 Received VehicleCreate: $vehicle")
        this.vehicleService
            .createVehicle(vehicle)
            .log()
            .subscribe()
    }

    @KafkaListener(topics = ["vehicle-update"], groupId = "data-service")
    fun updateVehicle(vehicle: VehicleBoundary){
        println("Received VehicleUpdate: $vehicle")
        if (vehicle.stayDuration!! > 600)
            vehicle.alert = true
            
        this.vehicleService.updateVehicle(vehicle.id!!, vehicle)
            .doOnNext { updatedVehicle ->
                kafkaVehicleSender.sendUpdatedVehicleState(updatedVehicle)
            }
            .log()
            .subscribe()
    }

    @KafkaListener(topics = ["alert-created"], groupId = "data-service")
    fun createAlert(alert: AlertBoundary){
        println("Received alert: $alert")
        this.alertService
            .createAlert(alert)
            .log()
            .subscribe()
    }
}