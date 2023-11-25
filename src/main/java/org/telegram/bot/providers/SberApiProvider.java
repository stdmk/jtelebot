package org.telegram.bot.providers;

import org.telegram.bot.enums.SberScope;

public interface SberApiProvider {
    SberScope getScope();
}
