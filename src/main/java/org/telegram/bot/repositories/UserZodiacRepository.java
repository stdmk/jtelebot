package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserZodiac;

@Repository
public interface UserZodiacRepository extends JpaRepository<UserZodiac, Long> {
    UserZodiac findByChatAndUser(Chat chat, User user);
}
