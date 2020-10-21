package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Speech;

import java.util.List;

public interface SpeechRepository extends JpaRepository<Speech, Long> {

    List<Speech> findByTag(String tag);
}
