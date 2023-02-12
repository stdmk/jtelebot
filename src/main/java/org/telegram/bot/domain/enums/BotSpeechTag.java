package org.telegram.bot.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BotSpeechTag {
    COMMAND_FOR_GROUP_CHATS("commandForGroupChats"),
    DATA_BASE_INTEGRITY("databaseIntegrity"),
    ECHO("echo"),
    NO_RESPONSE("noResponse"),
    NO_ACCESS("noAccess"),
    NOT_OWNER("notOwner"),
    SAVED("saved"),
    UNABLE_TO_FIND_TOKEN("unableToFindToken"),
    USER_NOT_FOUND("userNotFound"),
    WRONG_INPUT("wrongInput"),
    INTERNAL_ERROR("internalError"),
    FOUND_NOTHING("foundNothing"),
    SETTING_REQUIRED("settingRequired"),
    DUPLICATE_ENTRY("duplicateEntry"),
    TOO_BIG_FILE("tooBigFile"),
    ;

    private final String value;
}
