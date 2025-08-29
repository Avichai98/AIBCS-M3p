package app.alertservice.email

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    fun sendEmailHtml(
        to: String,
        subject: String,
        bodyHtml: String
    ): Mono<Void> {
        return Mono.fromRunnable<Void> {
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(bodyHtml, true) // HTML enabled

            println(">>> About to send HTML email")
            mailSender.send(mimeMessage)
            println(">>> Email send() returned")
        }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { e ->
                System.err.println("❌ Error sending email: ${e.message}")
                e.printStackTrace()
            }
            .then()
    }
}
