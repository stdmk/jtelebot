package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Token;
import org.telegram.bot.repositories.TokenRepository;
import org.telegram.bot.services.TokenService;

@Service
@AllArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);

    private final TokenRepository tokenRepository;

    @Override
    public Token save(Token token) {
        log.debug("Request to save Token: {} ", token);
        return tokenRepository.save(token);
    }

    @Override
    public Token get(String name) {
        log.debug("Request to get Token by name: {} ", name);
        return tokenRepository.findByName(name);
    }

    @Override
    public Token get(Long id) throws Exception {
        log.debug("Request to get Token by id: {} ", id);
        return tokenRepository.findById(id).orElseThrow(() -> new Exception("Unable to find Token with id " + id));
    }
}
