package org.telegram.bot.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.entities.Token;
import org.telegram.bot.repositories.TokenRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebTokenServiceImplTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private WebTokenServiceImpl webTokenService;

    @Test
    void createOrUpdateTokenWhenNotExistsTest() {
        when(tokenRepository.findByName("web-user-1")).thenReturn(null);
        when(tokenRepository.save(org.mockito.ArgumentMatchers.any(Token.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String token = webTokenService.createOrUpdateToken(1L);

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        Token savedToken = tokenCaptor.getValue();
        assertEquals("web-user-1", savedToken.getName());
        assertEquals(token, savedToken.getToken());
        assertNotNull(savedToken.getDescription());
    }

    @Test
    void getByTokenValueTest() {
        Token expectedToken = new Token().setToken("abc");
        when(tokenRepository.findByToken("abc")).thenReturn(expectedToken);

        assertEquals(expectedToken, webTokenService.getByTokenValue("abc"));
    }
}
