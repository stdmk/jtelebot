package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.NewsSource;

/**
 * Spring Data repository for the News entity.
 */
public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {
    NewsSource findByName(String name);
    NewsSource findFirstByNameOrUrl(String name, String url);
}
