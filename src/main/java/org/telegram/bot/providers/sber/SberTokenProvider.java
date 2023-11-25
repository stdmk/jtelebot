package org.telegram.bot.providers.sber;

import org.telegram.bot.enums.SberScope;
import org.telegram.bot.exception.GettingSberAccessTokenException;

public interface SberTokenProvider {
    String getToken(SberScope sberScope) throws GettingSberAccessTokenException;
}
