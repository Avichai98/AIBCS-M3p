package app.alertservice.email

import jakarta.mail.MessagingException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    @Autowired private val mailSender: JavaMailSender
) {

    fun sendEmail(to: String, subject: String, body: String) {
        try {
            val message = SimpleMailMessage()
            message.setTo(to)
            message.subject = subject
            message.text = body
            mailSender.send(message)
            println("Email sent to $to")
        } catch (e: MessagingException) {
            println("Failed to send email: ${e.message}")
            e.printStackTrace()
        }
    }
}
