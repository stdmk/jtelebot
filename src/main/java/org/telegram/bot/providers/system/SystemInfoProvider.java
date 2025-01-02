package org.telegram.bot.providers.system;

public interface SystemInfoProvider {
    long getMaxMemory();
    long getTotalMemory();
    long getFreeMemory();
    long getDbFileSize();
    long getFreeSystemSpace();
}
