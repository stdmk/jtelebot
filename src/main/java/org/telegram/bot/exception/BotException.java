package org.telegram.bot.exception;

public class BotException extends RuntimeException {
    public BotException(String message) {
        super(message);
    }
}
