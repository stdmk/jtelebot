package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TalkerUserSettings;
import org.telegram.bot.domain.entities.User;

public interface TalkerUserSettingsRepository extends JpaRepository<TalkerUserSettings, Long> {
    TalkerUserSettings getByUser(User user);
}
