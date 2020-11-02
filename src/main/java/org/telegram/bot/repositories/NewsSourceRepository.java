package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.NewsSource;

/**
 * Spring Data repository for the NewsSource entity.
 */
public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {
    NewsSource findByUrl(String url);
}
