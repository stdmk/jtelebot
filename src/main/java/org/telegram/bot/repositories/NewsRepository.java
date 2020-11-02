package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.domain.entities.NewsSource;

import java.util.List;

/**
 * Spring Data repository for the News entity.
 */
public interface NewsRepository extends JpaRepository<News, Long> {
    News findByChatAndId(Chat chat, Long newsSourceId);
    News findByChatAndName(Chat chat, String name);
    List<News> findByChat(Chat chat);
    List<News> findByNewsSource(NewsSource newsSource);
}
