package org.telegram.bot.mapper.bot.request;

import org.apache.commons.lang3.tuple.Pair;
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

class AttachmentMapperTest {

    private static final String DEFAULT_PHOTO_NAME = "photo";
    private static final String DEFAULT_VIDEO_NOTE_NAME = "video_note";
    private static final String DEFAULT_VOICE_NAME = "voice";

    private final AttachmentMapper attachmentMapper = new AttachmentMapper();

    @ParameterizedTest
    @MethodSource("provideMessages")
    void toAttachmentsTest(Message message, MessageContentType expectedMessageContentType, List<Attachment> expectedAttachments) {
        Pair<MessageContentType, List<Attachment>> attachments = attachmentMapper.toAttachments(message);

        assertEquals(expectedMessageContentType, attachments.getKey());

        List<Attachment> actualAttachments = attachments.getValue();

        if (expectedAttachments.isEmpty()) {
            assertTrue(actualAttachments.isEmpty());
        } else {
            assertEquals(expectedAttachments.size(), actualAttachments.size());

            for (int i = 0; i < actualAttachments.size(); i++) {
                Attachment expected = expectedAttachments.get(i);
                Attachment actual = actualAttachments.get(i);

                assertEquals(expected.getMimeType(), actual.getMimeType());
                assertEquals(expected.getFileUniqueId(), actual.getFileUniqueId());
                assertEquals(expected.getFileId(), actual.getFileId());
                assertEquals(expected.getFile(), actual.getFile());
                assertEquals(expected.getName(), actual.getName());
                assertEquals(expected.getSize(), actual.getSize());
                assertEquals(expected.getDuration(), actual.getDuration());
                assertEquals(expected.getText(), actual.getText());
            }
        }
    }

    private static Stream<Arguments> provideMessages() {
        final String fileUniqueId1 = "fileUniqueId1";
        final String fileId1 = "fileId1";
        final String name1 = "name1";
        final int intSize1 = 10;
        final long longSize1 = 10L;
        final int duration1 = 11;
        final String text1 = "text1";
        final String mimeType1 = "mimeType1";
        final String fileUniqueId2 = "fileUniqueId2";
        final String fileId2 = "fileId2";
        final int intSize2 = 20;
        final long longSize2 = 20L;

        Message textMessage = new Message();
        textMessage.setText("test");

        Sticker videoSticker = new Sticker();
        videoSticker.setIsVideo(true);
        videoSticker.setFileUniqueId(fileUniqueId1);
        videoSticker.setFileId(fileId1);
        videoSticker.setSetName(name1);
        videoSticker.setFileSize(intSize1);
        videoSticker.setEmoji(text1);
        Message videoStrickerMessage = new Message();
        videoStrickerMessage.setSticker(videoSticker);
        Attachment videoStickerAttachment = new Attachment()
                .setMimeType("video/webm")
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(name1)
                .setSize(longSize1)
                .setText(text1);

        Sticker animatedSticker = new Sticker();
        animatedSticker.setIsAnimated(true);
        animatedSticker.setFileUniqueId(fileUniqueId1);
        animatedSticker.setFileId(fileId1);
        animatedSticker.setSetName(name1);
        animatedSticker.setFileSize(intSize1);
        animatedSticker.setEmoji(text1);
        Message animatedStrickerMessage = new Message();
        animatedStrickerMessage.setSticker(animatedSticker);
        Attachment animatedStickerAttachment = new Attachment()
                .setMimeType("application/x-tgsticker")
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(name1)
                .setSize(longSize1)
                .setText(text1);

        Sticker sticker = new Sticker();
        sticker.setFileUniqueId(fileUniqueId1);
        sticker.setFileId(fileId1);
        sticker.setSetName(name1);
        sticker.setFileSize(intSize1);
        sticker.setEmoji(text1);
        Message stickerMessage = new Message();
        stickerMessage.setSticker(sticker);
        Attachment stickerAttachment = new Attachment()
                .setMimeType("image/webp")
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(name1)
                .setSize(longSize1)
                .setText(text1);

        PhotoSize photo1 = new PhotoSize();
        photo1.setFileId(fileId1);
        photo1.setFileUniqueId(fileUniqueId1);
        photo1.setFileSize(intSize1);
        PhotoSize photo2 = new PhotoSize();
        photo2.setFileId(fileId2);
        photo2.setFileUniqueId(fileUniqueId2);
        photo2.setFileSize(intSize2);
        Message photoMessage = new Message();
        photoMessage.setPhoto(List.of(photo1, photo2));
        Attachment photoAttachment1 = new Attachment()
                .setMimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(DEFAULT_PHOTO_NAME)
                .setSize(longSize1);
        Attachment photoAttachment2 = new Attachment()
                .setMimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                .setFileUniqueId(fileUniqueId2)
                .setFileId(fileId2)
                .setName(DEFAULT_PHOTO_NAME)
                .setSize(longSize2);

        Animation animation = new Animation(fileId1, fileUniqueId1, 1, 1, duration1);
        animation.setMimeType(mimeType1);
        animation.setFileName(name1);
        animation.setFileSize(longSize1);
        Message animationMessage = new Message();
        animationMessage.setAnimation(animation);
        Attachment animationAttachment = new Attachment()
                .setMimeType(mimeType1)
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(name1)
                .setSize(longSize1)
                .setDuration(duration1);

        Audio audio = new Audio();
        audio.setFileId(fileId1);
        audio.setFileUniqueId(fileUniqueId1);
        audio.setMimeType(mimeType1);
        audio.setFileName(name1);
        audio.setFileSize(longSize1);
        audio.setDuration(duration1);
        audio.setPerformer(text1);
        Message audioMessage = new Message();
        audioMessage.setAudio(audio);
        Attachment audioAttachment = new Attachment()
                .setMimeType(mimeType1)
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(name1)
                .setSize(longSize1)
                .setDuration(duration1)
                .setText(text1);

        Document document = new Document();
        document.setMimeType(mimeType1);
        document.setFileUniqueId(fileUniqueId1);
        document.setFileId(fileId1);
        document.setFileName(name1);
        document.setFileSize(longSize1);
        Message documentMessage = new Message();
        documentMessage.setDocument(document);
        Attachment documentAttachment = new Attachment()
                .setMimeType(mimeType1)
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(name1)
                .setSize(longSize1);

        Video video = new Video();
        video.setFileId(fileId1);
        video.setFileUniqueId(fileUniqueId1);
        video.setMimeType(mimeType1);
        video.setFileName(name1);
        video.setFileSize(longSize1);
        video.setDuration(duration1);
        Message videoMessage = new Message();
        videoMessage.setVideo(video);
        Attachment videoAttachment = new Attachment()
                .setMimeType(mimeType1)
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(name1)
                .setSize(longSize1)
                .setDuration(duration1);

        VideoNote videoNote = new VideoNote();
        videoNote.setFileUniqueId(fileUniqueId1);
        videoNote.setFileId(fileId1);
        videoNote.setFileSize(intSize1);
        videoNote.setDuration(duration1);
        Message videoNoteMessage = new Message();
        videoNoteMessage.setVideoNote(videoNote);
        Attachment videoNoteAttachment = new Attachment()
                .setMimeType("video/mp4")
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(DEFAULT_VIDEO_NOTE_NAME)
                .setSize(longSize1)
                .setDuration(duration1);

        Voice voice = new Voice();
        voice.setFileUniqueId(fileUniqueId1);
        voice.setFileId(fileId1);
        voice.setMimeType(mimeType1);
        voice.setFileSize(longSize1);
        voice.setDuration(duration1);
        Message voiceMessage = new Message();
        voiceMessage.setVoice(voice);
        Attachment voiceNoteAttachment = new Attachment()
                .setMimeType(mimeType1)
                .setFileUniqueId(fileUniqueId1)
                .setFileId(fileId1)
                .setName(DEFAULT_VOICE_NAME)
                .setSize(longSize1)
                .setDuration(duration1);

        Message message = new Message();

        return Stream.of(
                Arguments.of(textMessage, MessageContentType.TEXT, List.of()),
                Arguments.of(videoStrickerMessage, MessageContentType.STICKER, List.of(videoStickerAttachment)),
                Arguments.of(animatedStrickerMessage, MessageContentType.STICKER, List.of(animatedStickerAttachment)),
                Arguments.of(stickerMessage, MessageContentType.STICKER, List.of(stickerAttachment)),
                Arguments.of(photoMessage, MessageContentType.PHOTO, List.of(photoAttachment1, photoAttachment2)),
                Arguments.of(animationMessage, MessageContentType.ANIMATION, List.of(animationAttachment)),
                Arguments.of(audioMessage, MessageContentType.AUDIO, List.of(audioAttachment)),
                Arguments.of(documentMessage, MessageContentType.FILE, List.of(documentAttachment)),
                Arguments.of(videoMessage, MessageContentType.VIDEO, List.of(videoAttachment)),
                Arguments.of(videoNoteMessage, MessageContentType.VIDEO_NOTE, List.of(videoNoteAttachment)),
                Arguments.of(voiceMessage, MessageContentType.VOICE, List.of(voiceNoteAttachment)),
                Arguments.of(message, MessageContentType.UNKNOWN, List.of())
        );
    }

}