package app.alertservice.services

import app.alertservice.boundaries.AlertBoundary
import app.alertservice.email.EmailService
import app.alertservice.interfaces.AlertService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class AlertServiceImpl(
    private val emailService: EmailService
) : AlertService {
    lateinit var dataServiceUrl: String
    lateinit var webClient: WebClient

    @Value("\${remote.alerts.service.url: http://data-management-service:8080/alerts}")
    fun setRemoteUrl(url: String) {
        this.dataServiceUrl = url
    }

    @PostConstruct
    fun init() {
        System.err.println("***** $dataServiceUrl")
        this.webClient = WebClient.create(dataServiceUrl)
    }

    override fun createAlert(alert: AlertBoundary, authorizationHeader: String): Mono<AlertBoundary> {
        return this.webClient
            .post()
            .uri("/create")
            .header("Authorization", authorizationHeader)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(alert)
            .retrieve()
            .bodyToMono(AlertBoundary::class.java)
            .doOnError { e -> e.printStackTrace() }.log()
    }

    override fun sendAlert(alert: AlertBoundary): Mono<Void> {
        return emailService.sendEmail(
            to = "tchjha2@gmail.com",
            subject = "Test email from alert process",
            body = "This is a test email from the alert workflow."
        ).onErrorResume { e ->
                println("Failed to send alert email: ${e.message}")
                Mono.empty()
            }

    }

}