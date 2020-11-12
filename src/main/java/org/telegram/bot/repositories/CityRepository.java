package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.City;

/**
 * Spring Data repository for the City entity.
 */

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
}
