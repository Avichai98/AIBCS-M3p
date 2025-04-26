package app.alertservice.kafka

import app.alertservice.boundaries.AlertBoundary
import app.alertservice.interfaces.AlertService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class AlertKafkaConsumer(
    private val alertService: AlertService
) {

    @KafkaListener(topics = ["violations"], groupId = "alert-service-group")
    fun listen(alert: AlertBoundary) {
        this.alertService.
        createAlert(alert)
            .flatMap { alertService.sendAlert(it) }
            .subscribe()
    }
}
