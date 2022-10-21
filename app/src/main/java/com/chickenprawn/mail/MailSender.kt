package com.chickenprawn.mail

import android.content.Context
import android.util.Log
import com.chickenprawn.studip.StudIpFile
import com.chickenprawn.util.saveStudIpAsTempFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MailSender() {

    companion object {
        private val mailSender: MailSender? = null

        operator fun invoke(): MailSender{
            return mailSender ?: MailSender()
        }
    }

    private val senderAddress = "mailerstudip@gmail.com" // Gmail Email Address
    private val senderPassword = "tjujnmwjwcucwmgf" // OAuth Password
    private val session: Session

    init {
        val properties = Properties()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.host"] = "smtp.gmail.com"
        properties["mail.smtp.port"] = "587"

        session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(senderAddress, senderPassword)
            }
        })

    }

    fun downloadAndSend(context: Context, recipient: String, file: StudIpFile){
        CoroutineScope(Dispatchers.IO).launch {
            val tempFile = saveStudIpAsTempFile(context, file)
            sendFile(recipient, tempFile, file.name)
            tempFile.delete()
        }
    }

    fun sendFile(recipient: String, file: File, otherName: String? = null){
        // Because of bad temp filenames check if anotherName was set
        val fileName = otherName ?: file.name

        // Check if email address is valid
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(recipient).matches()){
            throw IllegalArgumentException("This method only accepts valid addresses!" +
                    " Checking must be done before!")
        }

        val message = MimeMessage(session) // Main message wrapper

        message.setRecipient(Message.RecipientType.TO, InternetAddress(recipient))
        message.subject = "Document: $fileName"

        val multipart = MimeMultipart();

        // Email Body
        val messageBodyPart = MimeBodyPart();
        messageBodyPart.setText("Document sent from the StudIpMailer Android App") // Body text of the email
        multipart.addBodyPart(messageBodyPart);

        // Attachment
        val attachment: BodyPart = MimeBodyPart()
        val source = FileDataSource(file)
        attachment.dataHandler = DataHandler(source)
        attachment.fileName = fileName

        multipart.addBodyPart(attachment)

        message.setContent(multipart)
        Transport.send(message)

        Log.d("StudIpMailer", "Email with document '${file.name}' sent")
    }

}