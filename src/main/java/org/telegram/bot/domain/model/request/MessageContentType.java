package org.telegram.bot.domain.model.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MessageContentType {
    TEXT("${enum.messagecontenttype.text}"),
    STICKER("${enum.messagecontenttype.sticker}"),
    PHOTO("${enum.messagecontenttype.photo}"),
    ANIMATION("${enum.messagecontenttype.animation}"),
    AUDIO("${enum.messagecontenttype.audio}"),
    FILE("${enum.messagecontenttype.file}"),
    VIDEO("${enum.messagecontenttype.video}"),
    VIDEO_NOTE("${enum.messagecontenttype.videonote}"),
    VOICE("${enum.messagecontenttype.voice}"),
    REACTION("${enum.messagecontenttype.reaction}"),
    UNKNOWN("${enum.messagecontenttype.unknown}"),
    ;

    private final String name;
}
