package org.telegram.bot.services.email;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.mapper.email.request.EmailMessageMapper;
import org.telegram.bot.services.BotStats;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
@ConditionalOnPropertyNotEmpty("mail.smtp.host")
public class EmailSender {

    private final EmailMessageMapper emailMessageMapper;
    private final BotStats botStats;

    @Async
    public void sendMail(EmailResponse emailResponse) {
        try {
            Message emailMessage = emailMessageMapper.toEmailMessage(emailResponse);
            Transport.send(emailMessage);
        } catch (MessagingException | IOException e) {
            String errorMessage = "Failed to send email message: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(emailResponse, e, errorMessage);
        }
    }

}

