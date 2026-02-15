package org.telegram.bot.mapper.email.request;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MimeBodyPartMapperTest {

    private final MimeBodyPartMapper mimeBodyPartMapper = new MimeBodyPartMapper();

    @Test
    void toAttachmentPartIoFileTest() throws MessagingException, IOException {
        final String fileName = "fileName";

        java.io.File file = mock(java.io.File.class);
        when(file.getName()).thenReturn(fileName);

        MimeBodyPart attachmentPart = mimeBodyPartMapper.toAttachmentPart(file);

        assertNotNull(attachmentPart);
        assertEquals(fileName, attachmentPart.getFileName());
    }

    @Test
    void toAttachmentPartFileTest() throws MessagingException, IOException {
        final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        final String name = "name";
        File attachment = new File(FileType.FILE, content, name);

        MimeBodyPart attachmentPart = mimeBodyPartMapper.toAttachmentPart(attachment);

        assertEquals("text/plain", attachmentPart.getContentType());
        assertEquals(name, attachmentPart.getFileName());

        byte[] actualContent = attachmentPart.getInputStream().readAllBytes();
        assertArrayEquals(content, actualContent);
    }

    @Test
    void toAttachmentPartTest() throws MessagingException, IOException {
        final byte[] content = "content".getBytes(StandardCharsets.UTF_8);

        MimeBodyPart attachmentPart = mimeBodyPartMapper.toAttachmentPart(content, null, "text/plain");

        assertEquals("text/plain", attachmentPart.getContentType());
        assertEquals("file", attachmentPart.getFileName());

        byte[] actualContent = attachmentPart.getInputStream().readAllBytes();
        assertArrayEquals(content, actualContent);
    }

}