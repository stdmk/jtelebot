package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.Error;

public interface ErrorRepository extends JpaRepository<Error, Long> {
}
