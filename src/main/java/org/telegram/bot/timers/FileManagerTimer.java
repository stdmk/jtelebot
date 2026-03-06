package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.BotStats;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class FileManagerTimer extends TimerParent {

    @Value("${temporaryFileLifetimeSeconds:900}")
    private Integer temporaryFileLifetimeSeconds;

    private static final AtomicInteger FILES_COUNTER = new AtomicInteger();
    private static final Map<String, LocalDateTime> FILES = new HashMap<>();

    private final BotStats botStats;

    @Override
    @Scheduled(fixedRate = 30000)
    public void execute() {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Set<String> fileNamesToRemove = new HashSet<>();

        FILES.forEach((key, value) -> {
            if (dateTimeNow.isAfter(value.plusSeconds(temporaryFileLifetimeSeconds)) && (deleteFileFromDisk(key))) {
                fileNamesToRemove.add(key);
            }
        });

        deleteFiles(fileNamesToRemove);
    }

    public String addFile(String prefix, String postfix) {
        String fileName = prefix + FILES_COUNTER.incrementAndGet() + postfix;

        File file = new File(fileName);
        if (file.exists() && (!deleteFileFromDisk(file))) {
            return addFile(prefix, postfix);
        }

        FILES.put(fileName, LocalDateTime.now());
        return fileName;
    }

    public void deleteFile(String fileName) {
        FILES.remove(fileName);
    }

    public void deleteFiles(Set<String> fileNames) {
        FILES.keySet().removeAll(fileNames);
    }

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
