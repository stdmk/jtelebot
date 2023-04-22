package org.telegram.bot.services;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

public interface SpyModeService {
    SendMessage generateMessage(User user, String textMessage);
}
