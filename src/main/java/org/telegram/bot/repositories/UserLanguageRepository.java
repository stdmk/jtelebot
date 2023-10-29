package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserLanguage;

public interface UserLanguageRepository extends JpaRepository<UserLanguage, Long> {
    UserLanguage findByUserAndChat(User user, Chat chat);
}
