package org.telegram.bot.providers.sber;

import org.telegram.bot.enums.SberScope;

public interface SberApiProvider {
    SberScope getScope();
}
