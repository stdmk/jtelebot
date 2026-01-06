package org.telegram.bot.mapper.email.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.List;

import static org.telegram.bot.utils.FileUtils.getMimeType;

@RequiredArgsConstructor
@Component
@Slf4j
@ConditionalOnPropertyNotEmpty("mail.imaps.host")
public class EmailMessageMapper {

    private final BotStats botStats;
    @Lazy
    private final Bot bot;
    private final NetworkUtils networkUtils;
    private final MimeBodyPartMapper mimeBodyPartMapper;
    private final InternetAddress botEmailAddress;
    private final Session smtpSession;

    public Message toEmailMessage(EmailResponse emailResponse) throws MessagingException, IOException {
        MimeMessage message = new MimeMessage(smtpSession);
        message.setFrom(botEmailAddress);

        for (String emailAddress : emailResponse.getEmailAddresses()) {
            for (InternetAddress internetAddress : InternetAddress.parse(emailAddress)) {
                message.addRecipient(MimeMessage.RecipientType.TO, internetAddress);
            }
        }

        message.setSubject(emailResponse.getSubject());

        MimeMultipart multipart = new MimeMultipart("mixed");

        StringBuilder messageText = new StringBuilder();
        if (emailResponse.getText() != null) {
            messageText.append(normalizeText(emailResponse.getText()));
        }

        List<File> attachments = emailResponse.getAttachments();
        if (attachments != null) {
            for (File attachment : attachments) {
                if (attachment.getBytes() != null) {
                    multipart.addBodyPart(mimeBodyPartMapper.toAttachmentPart(attachment));
                } else if (attachment.getDiskFile() != null) {
                    multipart.addBodyPart(mimeBodyPartMapper.toAttachmentPart(attachment.getDiskFile()));
                } else if (attachment.getFileId() != null) {
                    byte[] bytes;
                    try {
                        bytes = bot.getBytesTelegramFile(attachment.getFileId());
                        multipart.addBodyPart(mimeBodyPartMapper.toAttachmentPart(bytes, "file", getMimeType(bytes)));
                    } catch (TelegramApiException e) {
                        String errorMessage = "Failed to get file from telegram: " + e.getMessage();
                        log.error(errorMessage);
                        botStats.incrementErrors(attachment.getFileId(), e, errorMessage);
                    }
                } else if (attachment.getUrl() != null) {
                    String url = attachment.getUrl();
                    try {
                        byte[] bytes = networkUtils.getFileFromUrl(url);
                        multipart.addBodyPart((mimeBodyPartMapper.toAttachmentPart(bytes, getFileName(attachment), getMimeType(bytes))));
                    } catch (IOException e) {
                        String errorMessage = "Filed to get file from url " + url + ": " + e.getMessage();
                        log.error(errorMessage);
                        messageText.append("<br><br>").append(url);
                    }
                }
            }
        }

        MimeBodyPart textPart = new MimeBodyPart();

        textPart.setContent(messageText.toString(), "text/html; charset=UTF-8");
        multipart.addBodyPart(textPart);

        message.setContent(multipart);

        return message;
    }

    private String getFileName(File attachment) {
        if (attachment.getName() != null) {
            return attachment.getName();
        }

        return TextUtils.getFileNameFromUrl(attachment.getUrl());
    }

    private String normalizeText(String text) {
        return text.replace("\n", "<br>");
    }

}
