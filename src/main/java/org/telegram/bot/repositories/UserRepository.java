package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUserId(Integer userId);
    User findByUsername(String username);
}
