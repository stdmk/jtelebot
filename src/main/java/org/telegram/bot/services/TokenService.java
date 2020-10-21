package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Token;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.Token}.
 */

public interface TokenService {

    /**
     * Save a Token.
     *
     * @param token the entity to save.
     * @return the persisted entity.
     */
    Token save(Token token);

    /**
     * Get a Token.
     *
     * @param name name of token to get.
     * @return the persisted entity.
     */
    Token get(String name);

    /**
     * Get a Token.
     *
     * @param id id of token to get.
     * @return the persisted entity.
     */
    Token get(Long id) throws Exception;
}
