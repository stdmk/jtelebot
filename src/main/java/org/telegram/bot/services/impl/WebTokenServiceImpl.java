package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Token;
import org.telegram.bot.repositories.TokenRepository;
import org.telegram.bot.services.WebTokenService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebTokenServiceImpl implements WebTokenService {

    private static final String TOKEN_NAME_PREFIX = "web-user-";

    private final TokenRepository tokenRepository;

    @Override
    public String createOrUpdateToken(Long userId) {
        String tokenName = TOKEN_NAME_PREFIX + userId;

        Token token = tokenRepository.findByName(tokenName);
        if (token == null) {
            token = new Token().setName(tokenName);
        }

        String tokenValue = UUID.randomUUID().toString();
        token.setToken(tokenValue);
        token.setDescription("Web auth token for user " + userId);

        return tokenRepository.save(token).getToken();
    }

    @Override
    public Token getByTokenValue(String tokenValue) {
        return tokenRepository.findByToken(tokenValue);
    }
}
