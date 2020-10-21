package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.Token;

/**
 * Spring Data repository for the Token entity.
 */

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Token findByName(String name);
}
