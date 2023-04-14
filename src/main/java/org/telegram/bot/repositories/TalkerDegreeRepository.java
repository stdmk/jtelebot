package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TalkerDegree;

import java.util.List;

/**
 * Spring Data repository for the TalkerDegree entity.
 */
public interface TalkerDegreeRepository extends JpaRepository<TalkerDegree, Long> {
    TalkerDegree findByChat(Chat chat);
    List<TalkerDegree> findAllByChatIdleMinutesGreaterThan(int chatIdleMinutes);
}
