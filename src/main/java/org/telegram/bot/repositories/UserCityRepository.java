package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.UserCity;

/**
 * Spring Data repository for the UserCity entity.
 */

@Repository
public interface UserCityRepository extends JpaRepository<UserCity, Long> {
}
