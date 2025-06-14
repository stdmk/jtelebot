package org.telegram.bot.repositories.calories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.calories.EatenProduct;

public interface EatenProductRepository extends JpaRepository<EatenProduct, Long> {
}
