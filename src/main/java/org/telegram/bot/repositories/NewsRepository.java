package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.News;

/**
 * Spring Data repository for the News entity.
 */
public interface NewsRepository extends JpaRepository<News, Long> {
}
