package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.TemporaryFileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Service
@Slf4j
public class TemporaryFileManagerImpl implements TemporaryFileManager {

    @Value("${temporaryFileLifetimeSeconds:900}")
    private Integer temporaryFileLifetimeSeconds;

    private static final AtomicInteger FILES_COUNTER = new AtomicInteger();
    private static final Map<String, LocalDateTime> FILES = new HashMap<>();

    private final BotStats botStats;

    @Override
    public String addFile(String prefix, String postfix) {
        String fileName = prefix + FILES_COUNTER.incrementAndGet() + postfix;

        File file = new File(fileName);
        if (file.exists() && (!deleteFileFromDisk(file))) {
            return addFile(prefix, postfix);
        }

        FILES.put(fileName, LocalDateTime.now().plusSeconds(temporaryFileLifetimeSeconds));
        return fileName;
    }

    @Override
    public File get(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            FILES.remove(fileName);
            return null;
        }

        return file;
    }

    private void deleteFiles(Set<String> fileNames) {
        FILES.keySet().removeAll(fileNames);
    }

    @Override
    public void deleteAllFiles() {
        Set<String> fileNamesToRemove = new HashSet<>();

        FILES.forEach((key, value) -> {
            if (deleteFileFromDisk(key)) {
                fileNamesToRemove.add(key);
            }
        });

        deleteFiles(fileNamesToRemove);

        log.warn("Failed to delete files: {}", String.join(", ", FILES.keySet()));
    }

    @Override
    public void cleanup() {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Set<String> fileNamesToRemove = new HashSet<>();

        FILES.forEach((key, value) -> {
            if (value.isBefore(dateTimeNow) && (deleteFileFromDisk(key))) {
                fileNamesToRemove.add(key);
            }
        });

        deleteFiles(fileNamesToRemove);
    }

    private boolean deleteFileFromDisk(String name) {
        File file = new File(name);
        return deleteFileFromDisk(file);
    }

    private boolean deleteFileFromDisk(File file) {
        try {
            Files.delete(file.toPath());
        } catch (NoSuchFileException e) {
            log.error("File {} already deleted", file);
        } catch (IOException e) {
            log.error("Failed to delete file {}", file);
            botStats.incrementErrors(file, e, "Failed to delete file");
            return false;
        }

        return true;
    }

}
