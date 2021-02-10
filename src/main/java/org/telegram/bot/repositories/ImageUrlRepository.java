package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.telegram.bot.domain.entities.ImageUrl;

public interface ImageUrlRepository extends JpaRepository<ImageUrl, Long> {
    ImageUrl findFirstByUrl(String url);

    @Query("SELECT max(iu.id) FROM ImageUrl iu")
    Long getImageUrlsCount();
}
