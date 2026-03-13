package org.telegram.bot.services;

import org.telegram.bot.domain.entities.Token;

public interface WebTokenService {

    String createOrUpdateToken(Long userId);

    Token getByTokenValue(String tokenValue);

}
