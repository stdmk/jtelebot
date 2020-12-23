package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Wiki;

public interface WikiRepository extends JpaRepository<Wiki, Integer> {
}
