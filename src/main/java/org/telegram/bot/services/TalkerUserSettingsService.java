package org.telegram.bot.services;

import org.telegram.bot.domain.entities.TalkerUserSettings;
import org.telegram.bot.domain.entities.User;

public interface TalkerUserSettingsService {
    boolean doNotReply(User user);
    TalkerUserSettings get(User user);
    void save(TalkerUserSettings talkerUserSettings);
    void remove(TalkerUserSettings talkerUserSettings);
}
