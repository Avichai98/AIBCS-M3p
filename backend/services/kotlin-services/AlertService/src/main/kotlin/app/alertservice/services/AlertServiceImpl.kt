package app.alertservice.services

import app.alertservice.boundaries.AlertBoundary
import app.alertservice.email.EmailService
import app.alertservice.interfaces.AlertService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class AlertServiceImpl(
    private val kafkaTemplate: KafkaTemplate<String, AlertBoundary>
    , private val emailService: EmailService
) :
    AlertService {
    lateinit var dataServiceUrl: String
    lateinit var webClient: WebClient

    @Value("\${remote.alerts.service.url: http://localhost:8080/alerts}")
    fun setRemoteUrl(url: String) {
        this.dataServiceUrl = url
    }

    @PostConstruct
    fun init() {
        System.err.println("***** $dataServiceUrl")
        this.webClient = WebClient.create(dataServiceUrl)
    }

    override fun createAlert(alert: AlertBoundary): Mono<AlertBoundary> {
        return this.webClient
            .post()
            .uri("/create")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(alert)
            .retrieve()
            .bodyToMono(AlertBoundary::class.java)
            .log()
    }

    override fun sendAlert(alert: AlertBoundary): Mono<Void> {
        return Mono.fromRunnable<Unit> {
            emailService.sendEmail(
                to = "tchjha2@gmail.com",
                subject = "בדיקת שליחת מייל",
                body = "זהו מייל בדיקה מתהליך ההתראה."
            )
        }.then()
    }
}