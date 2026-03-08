package org.telegram.bot.services;

import jakarta.annotation.Nullable;

import java.io.File;
import java.time.LocalDateTime;

public interface TemporaryFileManager {
    String addFile(String prefix, String postfix);
    @Nullable File get(String fileName);
    void deleteAllFiles();
    void cleanup(LocalDateTime expirationDateTime);
}
