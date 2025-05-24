package app.alertservice.email

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {

    fun sendEmail(to: String, subject: String, body: String): Mono<Void> {
        return Mono.fromRunnable<Void> {
            val message = SimpleMailMessage().apply {
                setTo(to)
                this.subject = subject
                text = body
            }
            println(">>> About to send email: $message")
            mailSender.send(message)
            println(">>> Email send() returned")
        }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { e ->
                System.err.println("❌ Error sending email: ${e.message}")
                e.printStackTrace()
            }
            .then() // Return Mono<Void>
    }
}
