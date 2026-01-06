package org.telegram.bot.mapper.email.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailMessageMapperTest {

    private final BotStats botStats = Mockito.mock(BotStats.class);
    private final Bot bot = Mockito.mock(Bot.class);
    private final NetworkUtils networkUtils = Mockito.mock(NetworkUtils.class);
    private final MimeBodyPartMapper mimeBodyPartMapper = Mockito.mock(MimeBodyPartMapper.class);
    private final InternetAddress botEmailAddress = Mockito.mock(InternetAddress.class);
    private final Session smtpSession = Session.getDefaultInstance(new Properties());

    private final EmailMessageMapper emailMessageMapper = new EmailMessageMapper(botStats, bot, networkUtils, mimeBodyPartMapper, botEmailAddress, smtpSession);

    @Test
    void toEmailMessageWithoutAttachmentsTest() throws MessagingException, IOException {
        final String email1 = "email1@example.com";
        final String email2 = "email2@example.com";
        final String subject = "subject";
        final String text = "text";
        EmailResponse emailResponse = new EmailResponse()
                .setEmailAddresses(Set.of(email1, email2))
                .setSubject(subject)
                .setText(text);

        Message emailMessage = emailMessageMapper.toEmailMessage(emailResponse);

        Set<String> recipients = Arrays.stream(emailMessage.getRecipients(Message.RecipientType.TO)).map(Address::toString).collect(Collectors.toSet());
        assertEquals(2, recipients.size());
        assertTrue(recipients.contains(email1));
        assertTrue(recipients.contains(email2));

        assertEquals(subject, emailMessage.getSubject());

        Object content = emailMessage.getContent();
        assertTrue(content instanceof MimeMultipart);

        MimeMultipart multipart = (MimeMultipart) content;
        assertEquals(1, multipart.getCount());

        BodyPart textPart = multipart.getBodyPart(0);
        assertEquals(text, textPart.getContent().toString());
    }

    @Test
    void toEmailMessageTest() throws MessagingException, IOException, TelegramApiException {
        final String email1 = "email1@example.com";
        final String email2 = "email2@example.com";
        final String subject = "subject";
        final String text = "text";
        final String fileIdWithFail = "fileId1";
        final String fileId2 = "fileId2";
        final String fileUrlWithFail = "url1";
        final String url2 = "http://example.com/file.txt";
        final String getFileFromTelegramErrorMessage = "telegramError";
        final String getFileFromUrlErrorMessage = "urlError";
        final byte[] attachmentBytesBytes = "attachmentBytesBytes".getBytes(StandardCharsets.UTF_8);
        final byte[] attachmentFileIdBytes = "attachmentFileIdBytes".getBytes(StandardCharsets.UTF_8);
        final byte[] attachmentFileUrlBytes = "attachmentFileUrlBytes".getBytes(StandardCharsets.UTF_8);

        File attachmentBytes = new File(FileType.FILE, attachmentBytesBytes, "name");
        File attachmentDiskFile = new File(FileType.FILE, new java.io.File(""));
        File attachmentFileIdWithFail = new File(fileIdWithFail);
        File attachmentFileId = new File(fileId2);
        File attachmentUrlWithFail = new File(FileType.FILE, fileUrlWithFail);
        File attachmentUrl = new File(FileType.FILE, url2);

        TelegramApiException telegramApiException = new TelegramApiException(getFileFromTelegramErrorMessage);
        IOException ioException = new IOException(getFileFromUrlErrorMessage);
        when(bot.getBytesTelegramFile(fileIdWithFail)).thenThrow(telegramApiException);
        when(bot.getBytesTelegramFile(fileId2)).thenReturn(attachmentFileIdBytes);
        when(networkUtils.getFileFromUrl(fileUrlWithFail)).thenThrow(ioException);
        when(networkUtils.getFileFromUrl(url2)).thenReturn(attachmentFileUrlBytes);

        MimeBodyPart partFromBytes = mock(MimeBodyPart.class);
        MimeBodyPart partFromDiskFile = mock(MimeBodyPart.class);
        MimeBodyPart partFromTelegramFile = mock(MimeBodyPart.class);
        when(mimeBodyPartMapper.toAttachmentPart(attachmentBytes)).thenReturn(partFromBytes);
        when(mimeBodyPartMapper.toAttachmentPart(attachmentDiskFile.getDiskFile())).thenReturn(partFromDiskFile);
        when(mimeBodyPartMapper.toAttachmentPart(attachmentFileIdBytes, "file", "text/plain")).thenReturn(partFromTelegramFile);
        when(mimeBodyPartMapper.toAttachmentPart(attachmentFileUrlBytes, "file.txt", "text/plain")).thenReturn(partFromTelegramFile);

        EmailResponse emailResponse = new EmailResponse()
                .setEmailAddresses(Set.of(email1, email2))
                .setSubject(subject)
                .setText(text)
                .setAttachments(List.of(attachmentBytes, attachmentDiskFile, attachmentFileIdWithFail, attachmentFileId, attachmentUrlWithFail, attachmentUrl));

        Message emailMessage = emailMessageMapper.toEmailMessage(emailResponse);

        Set<String> recipients = Arrays.stream(emailMessage.getRecipients(Message.RecipientType.TO)).map(Address::toString).collect(Collectors.toSet());
        assertEquals(2, recipients.size());
        assertTrue(recipients.contains(email1));
        assertTrue(recipients.contains(email2));

        assertEquals(subject, emailMessage.getSubject());

        Object content = emailMessage.getContent();
        assertTrue(content instanceof MimeMultipart);

        MimeMultipart multipart = (MimeMultipart) content;
        assertEquals(5, multipart.getCount());

        Set<BodyPart> parts = new HashSet<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            parts.add(multipart.getBodyPart(i));
        }

        assertTrue(parts.contains(partFromBytes));
        assertTrue(parts.contains(partFromDiskFile));
        assertTrue(parts.contains(partFromTelegramFile));
        assertTrue(parts.contains(partFromTelegramFile));

        BodyPart textPart = multipart.getBodyPart(multipart.getCount() - 1);
        assertEquals(text + "<br><br>" + fileUrlWithFail, textPart.getContent().toString());

        verify(botStats).incrementErrors(fileIdWithFail, telegramApiException, "Failed to get file from telegram: " + getFileFromTelegramErrorMessage);
    }

}