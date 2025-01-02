package org.telegram.bot.providers.system;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class SystemInfoProviderImpl implements SystemInfoProvider {

    private static final File DB_FILE = new File("db.mv.db");

    @Override
    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    @Override
    public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    @Override
    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getDbFileSize() {
        return DB_FILE.length();
    }

    @Override
    public long getFreeSystemSpace() {
        return DB_FILE.getFreeSpace();
    }

}
