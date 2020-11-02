package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.NewsMessage;

/**
 * Spring Data repository for the NewsMessage entity.
 */
public interface NewsMessageRepository extends JpaRepository<NewsMessage, Long> {
}
