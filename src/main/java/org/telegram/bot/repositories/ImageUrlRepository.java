package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.ImageUrl;

public interface ImageUrlRepository extends JpaRepository<ImageUrl, Long> {
}
