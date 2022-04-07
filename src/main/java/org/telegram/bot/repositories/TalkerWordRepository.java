package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.TalkerWord;

import java.util.List;
import java.util.Set;

/**
 * Spring Data repository for the TalkerWord entity.
 */
public interface TalkerWordRepository extends JpaRepository<TalkerWord, Long> {
    Set<TalkerWord> findAllByWordInIgnoreCase(List<String> words);
}
