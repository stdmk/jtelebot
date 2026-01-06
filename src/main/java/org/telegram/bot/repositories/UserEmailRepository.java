package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserEmail;

import java.util.List;

@Repository
public interface UserEmailRepository extends JpaRepository<UserEmail, Long> {
    UserEmail findByUser(User user);
    List<UserEmail> findByUserIn(List<User> users);
    List<UserEmail> findByEmailIn(List<String> users);
}
