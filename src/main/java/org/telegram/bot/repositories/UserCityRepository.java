package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;

import java.util.List;

/**
 * Spring Data repository for the UserCity entity.
 */

@Repository
public interface UserCityRepository extends JpaRepository<UserCity, Long> {
    UserCity findByUserAndChatId(User user, Long chatId);
    List<UserCity> findByCity(City city);
}
