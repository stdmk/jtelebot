package org.telegram.bot.services;

import jakarta.annotation.Nullable;

import java.io.File;

public interface TemporaryFileManager {
    String addFile(String prefix, String postfix);
    @Nullable File get(String fileName);
    void deleteAllFiles();
    void cleanup();
}
