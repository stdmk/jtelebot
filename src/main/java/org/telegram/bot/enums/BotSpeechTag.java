package org.telegram.bot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BotSpeechTag {
    COMMAND_FOR_GROUP_CHATS("commandforgroupchats"),
    DATA_BASE_INTEGRITY("databaseintegrity"),
    ECHO("echo"),
    NO_RESPONSE("noresponse"),
    NO_ACCESS("noaccess"),
    NOT_OWNER("notowner"),
    SAVED("saved"),
    UNABLE_TO_FIND_TOKEN("unabletofindtoken"),
    USER_NOT_FOUND("usernotfound"),
    WRONG_INPUT("wronginput"),
    INTERNAL_ERROR("internalerror"),
    FOUND_NOTHING("foundnothing"),
    SETTING_REQUIRED("settingrequired"),
    DUPLICATE_ENTRY("duplicateentry"),
    TOO_BIG_FILE("toobigfile"),
    TOO_BIG_REQUEST("toobigrequest")
    ;

    private final String value;
}
