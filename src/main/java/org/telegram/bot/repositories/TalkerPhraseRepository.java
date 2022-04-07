package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TalkerPhrase;

import java.util.Set;

/**
 * Spring Data repository for the TalkerPhrase entity.
 */
public interface TalkerPhraseRepository extends JpaRepository<TalkerPhrase, Long> {
    Set<TalkerPhrase> findAllByPhraseInIgnoreCase(Set<String> phrases);
}
