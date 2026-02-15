package org.telegram.bot.mapper.bot.request;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.games.Animation;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;

import java.util.List;

@Component
public class AttachmentMapper {

    private static final String DEFAULT_PHOTO_NAME = "photo";
    private static final String DEFAULT_VIDEO_NOTE_NAME = "video_note";
    private static final String DEFAULT_VOICE_NAME = "voice";

    public Pair<MessageContentType, List<Attachment>> toAttachments(org.telegram.telegrambots.meta.api.objects.message.Message message) {
        if (message.hasText()) {
            return Pair.of(MessageContentType.TEXT, List.of());
        } else if (message.hasSticker()) {
            return Pair.of(MessageContentType.STICKER, List.of(toAttachment(message.getSticker())));
        } else if (message.hasPhoto()) {
            return Pair.of(MessageContentType.PHOTO, toAttachments(message.getPhoto()));
        } else if (message.hasAnimation()) {
            return Pair.of(MessageContentType.ANIMATION, List.of(toAttachment(message.getAnimation())));
        } else if (message.hasAudio()) {
            return Pair.of(MessageContentType.AUDIO, List.of(toAttachment(message.getAudio())));
        } else if (message.hasDocument()) {
            return Pair.of(MessageContentType.FILE, List.of(toAttachment(message.getDocument())));
        } else if (message.hasVideo()) {
            return Pair.of(MessageContentType.VIDEO, List.of(toAttachment(message.getVideo())));
        } else if (message.hasVideoNote()) {
            return Pair.of(MessageContentType.VIDEO_NOTE, (List.of(toAttachment(message.getVideoNote()))));
        } else if (message.hasVoice()) {
            return Pair.of(MessageContentType.VOICE, List.of(toAttachment(message.getVoice())));
        } else {
            return Pair.of(MessageContentType.UNKNOWN, List.of());
        }

    }

    private Attachment toAttachment(Document document) {
        return toAttachment(document.getMimeType(), document.getFileUniqueId(), document.getFileId(), document.getFileName(), document.getFileSize(), null, null);
    }

    private List<Attachment> toAttachments(List<PhotoSize> photoList) {
        return photoList.stream().map(this::toAttachment).toList();
    }

    private Attachment toAttachment(PhotoSize photo) {
        return toAttachment(MimeTypeUtils.IMAGE_JPEG_VALUE, photo.getFileUniqueId(), photo.getFileId(), DEFAULT_PHOTO_NAME, photo.getFileSize().longValue(), null, null);
    }

    private Attachment toAttachment(Video video) {
        return toAttachment(video.getMimeType(), video.getFileUniqueId(), video.getFileId(), video.getFileName(), video.getFileSize(), video.getDuration(), null);
    }

    private Attachment toAttachment(VideoNote videoNote) {
        return toAttachment("video/mp4", videoNote.getFileUniqueId(), videoNote.getFileId(), DEFAULT_VIDEO_NOTE_NAME, videoNote.getFileSize().longValue(), videoNote.getDuration(), null);
    }

    private Attachment toAttachment(Voice voice) {
        return toAttachment(voice.getMimeType(), voice.getFileUniqueId(), voice.getFileId(), DEFAULT_VOICE_NAME, voice.getFileSize(), voice.getDuration(), null);
    }

    private Attachment toAttachment(Audio audio) {
        return toAttachment(audio.getMimeType(), audio.getFileUniqueId(), audio.getFileId(), audio.getFileName(), audio.getFileSize(), audio.getDuration(), audio.getPerformer());
    }

    private Attachment toAttachment(Animation animation) {
        return toAttachment(animation.getMimeType(), animation.getFileUniqueId(), animation.getFileId(), animation.getFileName(), animation.getFileSize(), animation.getDuration(), null);
    }

    private Attachment toAttachment(Sticker sticker) {
        String mimeType;
        if (Boolean.TRUE.equals(sticker.getIsVideo())) {
            mimeType = "video/webm";
        } else if (Boolean.TRUE.equals(sticker.getIsAnimated())) {
            mimeType = "application/x-tgsticker";
        } else {
            mimeType = "image/webp";
        }

        return toAttachment(mimeType, sticker.getFileUniqueId(), sticker.getFileId(), sticker.getSetName(), sticker.getFileSize().longValue(), null, sticker.getEmoji());
    }

    private Attachment toAttachment(String mimeType, String fileUniqueId, String fileId, String name, Long size, Integer duration, String text) {
        return new Attachment(mimeType, fileUniqueId, fileId, null, name, size, duration, text);
    }

}
