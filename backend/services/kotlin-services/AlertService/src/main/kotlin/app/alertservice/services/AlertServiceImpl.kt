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
        val vehicle = alert.vehicleBoundary

        val bodyHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; color: #333;">
            <h2 style="color:#d9534f;">🚨 Parking Violation Detected</h2>
            <p>A vehicle has been detected staying longer than permitted.</p>
            
            <h3>Alert Details:</h3>
            <ul>
                <li><b>ID:</b> ${alert.id}</li>
                <li><b>Camera ID:</b> ${alert.cameraId}</li>
                <li><b>Type:</b> ${alert.type}</li>
                <li><b>Severity:</b> ${alert.severity}</li>
                <li><b>Description:</b> ${alert.description}</li>
                <li><b>Timestamp:</b> ${alert.timestamp}</li>
            </ul>
            
            <h3>Vehicle Details:</h3>
            <ul>
                <li><b>Type:</b> ${vehicle?.type ?: "Unknown"}</li>
                <li><b>Manufacturer:</b> ${vehicle?.manufacturer ?: "Unknown"}</li>
                <li><b>Color:</b> ${vehicle?.color ?: "Unknown"}</li>
                <li><b>Stay Duration:</b> ${vehicle?.stayDurationFormatted ?: (vehicle?.stayDuration?.toString() ?: "N/A")}</li>
                <li><b>Location:</b> ${vehicle?.latitude}, ${vehicle?.longitude}</li>
            </ul>
            
            ${if (vehicle?.imageUrl != null) """
                <p><b>Captured Image:</b></p>
                <img src="${vehicle.imageUrl}" 
                     alt="Vehicle image" 
                     style="max-width:500px; border:1px solid #ccc;"/>
            """ else "<p><i>No image available</i></p>"}
        </body>
        </html>
    """.trimIndent()

        return emailService.sendEmailHtml(
            to = "tchjha2@gmail.com",
            subject = "Parking Violation Alert",
            bodyHtml = bodyHtml
        ).onErrorResume { e ->
            println("Failed to send alert email: ${e.message}")
            Mono.empty()
        }
    }
}