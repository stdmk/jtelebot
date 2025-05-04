package org.telegram.bot.mapper.telegram.request;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.MimeTypeUtils;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.games.Animation;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttachmentMapperTest {

    private static final String FILE_UNIQUE_ID = "FileUniqueId";
    private static final String FILE_ID = "FileId";
    private static final Long FILE_SIZE = 256L;
    private static final Integer FILE_SIZE_INT = FILE_SIZE.intValue();

    private final AttachmentMapper attachmentMapper = new AttachmentMapper();

    @Test
    void toAttachmentsTextMessageTest() {
        Message message = mock(Message.class);

        when(message.hasText()).thenReturn(true);

        Pair<MessageContentType, List<Attachment>> pair = attachmentMapper.toAttachments(message);

        assertEquals(MessageContentType.TEXT, pair.getLeft());
        assertTrue(pair.getRight().isEmpty());
    }

    @Test
    void toAttachmentsUnknownTypeTest() {
        Message message = mock(Message.class);

        Pair<MessageContentType, List<Attachment>> pair = attachmentMapper.toAttachments(message);

        assertEquals(MessageContentType.UNKNOWN, pair.getLeft());
        assertTrue(pair.getRight().isEmpty());
    }

    @Test
    void toAttachmentsPhotoMessageTest() {
        final String fileId1 = "fileId1";
        final String fileId2 = "fileId2";
        final String fileUniqueId1 = "fileUniqueId1";
        final String fileUniqueId2 = "fileUniqueId2";
        final int fileSize1 = 1;
        final int fileSize2 = 2;
        Message message = mock(Message.class);
        List<PhotoSize> photoSizes = List.of(
                new PhotoSize(fileId1, fileUniqueId1, 0, 0, fileSize1, "path"),
                new PhotoSize(fileId2, fileUniqueId2, 0, 0, fileSize2, "path"));

        when(message.hasPhoto()).thenReturn(true);
        when(message.getPhoto()).thenReturn(photoSizes);

        Pair<MessageContentType, List<Attachment>> pair = attachmentMapper.toAttachments(message);

        assertEquals(MessageContentType.PHOTO, pair.getLeft());
        List<Attachment> attachments = pair.getRight();

        assertEquals(2, attachments.size());

        Attachment attachment1 = attachments.get(0);
        assertEquals(MimeTypeUtils.IMAGE_JPEG_VALUE, attachment1.getMimeType());
        assertEquals(fileId1, attachment1.getFileId());
        assertEquals(fileUniqueId1, attachment1.getFileUniqueId());
        assertEquals("photo", attachment1.getName());
        assertEquals(fileSize1, attachment1.getSize());

        Attachment attachment2 = attachments.get(1);
        assertEquals(MimeTypeUtils.IMAGE_JPEG_VALUE, attachment2.getMimeType());
        assertEquals(fileId2, attachment2.getFileId());
        assertEquals(fileUniqueId2, attachment2.getFileUniqueId());
        assertEquals("photo", attachment2.getName());
        assertEquals(fileSize2, attachment2.getSize());
    }

    @ParameterizedTest
    @MethodSource("provideApiObjects")
    void toAttachmentsStickerMessageTest(Message message, MessageContentType expectedMessageContentType, String expectedMimeType, String expectedName, Integer expectedDuration) {
        Pair<MessageContentType, List<Attachment>> pair = attachmentMapper.toAttachments(message);

        assertEquals(expectedMessageContentType, pair.getLeft());

        List<Attachment> attachments = pair.getRight();
        assertEquals(1, attachments.size());

        Attachment attachment = attachments.get(0);
        assertEquals(expectedMimeType, attachment.getMimeType());
        assertEquals(FILE_UNIQUE_ID, attachment.getFileUniqueId());
        assertEquals(FILE_ID, attachment.getFileId());
        assertEquals(expectedName, attachment.getName());
        assertEquals(FILE_SIZE, attachment.getSize());
        assertEquals(expectedDuration, attachment.getDuration());
    }

    private static Stream<Arguments> provideApiObjects() {
        return Stream.of(
                getStickerArguments(),
                getAnimationArguments(),
                getAudioArguments(),
                getVoiceArguments(),
                getVideoNoteArguments(),
                getVideoArguments(),
                getDocumentArguments()
        );
    }

    private static Arguments getStickerArguments() {
        Sticker sticker = new Sticker();
        sticker.setFileUniqueId(FILE_UNIQUE_ID);
        sticker.setFileId(FILE_ID);
        sticker.setSetName("setName");
        sticker.setFileSize(FILE_SIZE_INT);
        Message message = mock(Message.class);
        when(message.hasSticker()).thenReturn(true);
        when(message.getSticker()).thenReturn(sticker);

        return Arguments.of(message, MessageContentType.STICKER, null, sticker.getSetName(), null);
    }

    private static Arguments getAnimationArguments() {
        final int duration = 123;
        final String fileName = "fileName";
        final String mimeType = "mimeType";
        Animation animation = new Animation(FILE_ID, FILE_UNIQUE_ID, 0, 0, duration);
        animation.setFileName(fileName);
        animation.setMimeType(mimeType);
        animation.setFileSize(FILE_SIZE);
        Message message = mock(Message.class);
        when(message.hasAnimation()).thenReturn(true);
        when(message.getAnimation()).thenReturn(animation);

        return Arguments.of(message, MessageContentType.ANIMATION, mimeType, fileName, duration);
    }

    private static Arguments getAudioArguments() {
        final int duration = 123;
        final String fileName = "fileName";
        final String mimeType = "mimeType";
        Audio audio = new Audio(FILE_ID, FILE_UNIQUE_ID, duration, mimeType, FILE_SIZE, "title", "performer", new PhotoSize(), fileName);
        Message message = mock(Message.class);
        when(message.hasAudio()).thenReturn(true);
        when(message.getAudio()).thenReturn(audio);

        return Arguments.of(message, MessageContentType.AUDIO, mimeType, fileName, duration);
    }

    private static Arguments getVoiceArguments() {
        final int duration = 123;
        final String fileName = "voice";
        final String mimeType = "mimeType";
        Voice voice = new Voice(FILE_ID, FILE_UNIQUE_ID, duration, mimeType, FILE_SIZE);
        Message message = mock(Message.class);
        when(message.hasVoice()).thenReturn(true);
        when(message.getVoice()).thenReturn(voice);

        return Arguments.of(message, MessageContentType.VOICE, mimeType, fileName, duration);
    }

    private static Arguments getVideoNoteArguments() {
        final int duration = 123;
        final String fileName = "videonote";
        VideoNote videoNote = new VideoNote(FILE_ID, FILE_UNIQUE_ID, 321, duration, new PhotoSize(), FILE_SIZE_INT);
        Message message = mock(Message.class);
        when(message.hasVideoNote()).thenReturn(true);
        when(message.getVideoNote()).thenReturn(videoNote);

        return Arguments.of(message, MessageContentType.VIDEO_NOTE, null, fileName, duration);
    }

    private static Arguments getVideoArguments() {
        final int duration = 123;
        final String fileName = "fileName";
        final String mimeType = "mimeType";
        Video video = new Video(FILE_ID, FILE_UNIQUE_ID, 0, 0, duration, new PhotoSize(), mimeType, FILE_SIZE, fileName);
        Message message = mock(Message.class);
        when(message.hasVideo()).thenReturn(true);
        when(message.getVideo()).thenReturn(video);

        return Arguments.of(message, MessageContentType.VIDEO, mimeType, fileName, duration);
    }

    private static Arguments getDocumentArguments() {
        final String fileName = "fileName";
        final String mimeType = "mimeType";
        Document document = new Document(FILE_ID, FILE_UNIQUE_ID, new PhotoSize(), fileName, mimeType, FILE_SIZE);
        Message message = mock(Message.class);
        when(message.hasDocument()).thenReturn(true);
        when(message.getDocument()).thenReturn(document);

        return Arguments.of(message, MessageContentType.FILE, mimeType, fileName, null);
    }

}