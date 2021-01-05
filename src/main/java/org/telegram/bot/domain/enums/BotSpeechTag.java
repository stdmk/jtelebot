package org.telegram.bot.domain.enums;

import lombok.Getter;

@Getter
public enum BotSpeechTag {
    COMMAND_FOR_GROUP_CHATS("commandForGroupChats"),
    DATA_BASE_INTEGRITY("databaseIntegrity"),
    ECHO("echo"),
    NO_RESPONSE("noResponse"),
    NOT_OWNER("notOwner"),
    SAVED("saved"),
    UNABLE_TO_FIND_TOKEN("unableToFindToken"),
    USER_NOT_FOUND("userNotFound"),
    WRONG_INPUT("wrongInput");

    private final String value;

    BotSpeechTag(String value) {
        this.value = value;
    }
}
