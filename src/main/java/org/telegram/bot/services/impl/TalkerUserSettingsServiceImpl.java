package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TalkerUserSettings;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.TalkerUserSettingsRepository;
import org.telegram.bot.services.TalkerUserSettingsService;

@RequiredArgsConstructor
@Service
public class TalkerUserSettingsServiceImpl implements TalkerUserSettingsService {

    private final TalkerUserSettingsRepository talkerUserSettingsRepository;

    @Override
    public boolean doNotReply(User user) {
        TalkerUserSettings talkerUserSettings = this.get(user);
        return talkerUserSettings != null && Boolean.TRUE.equals(talkerUserSettings.getDoNotReply());
    }

    @Override
    public TalkerUserSettings get(User user) {
        return talkerUserSettingsRepository.getByUser(user);
    }

    @Override
    public void save(TalkerUserSettings talkerUserSettings) {
        talkerUserSettingsRepository.save(talkerUserSettings);
    }

    @Override
    public void remove(TalkerUserSettings talkerUserSettings) {
        talkerUserSettingsRepository.delete(talkerUserSettings);
    }
}
