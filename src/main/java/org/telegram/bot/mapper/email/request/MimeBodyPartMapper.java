package org.telegram.bot.mapper.email.request;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;

@Component
public class MimeBodyPartMapper {

    public MimeBodyPart toAttachmentPart(java.io.File file) throws MessagingException, IOException {
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.attachFile(file);
        attachmentPart.setFileName(file.getName());
        return attachmentPart;
    }

    public MimeBodyPart toAttachmentPart(File attachment) throws IOException, MessagingException {
        return toAttachmentPart(attachment.getBytes(), attachment.getName(), attachment.getFileType().getMimeType());
    }

    public MimeBodyPart toAttachmentPart(byte[] bytes, String name, String mimeType) throws IOException, MessagingException {
        DataSource ds = new ByteArrayDataSource(bytes, mimeType);

        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setDataHandler(new DataHandler(ds));
        attachmentPart.setFileName(name == null ? "file" : name);

        return attachmentPart;
    }

}
