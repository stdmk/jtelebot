package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserTv;

import java.util.List;

public interface UserTvRepository extends JpaRepository<UserTv, Long> {
    List<UserTv> findByChatAndUser(Chat chat, User user);
    UserTv findByChatAndUserAndTvChannel(Chat chat, User user, TvChannel tvChannel);
}
