package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
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

    private final static AtomicInteger filesCounter = new AtomicInteger();
    private final static Map<String, LocalDateTime> files = new HashMap<>();

    @Override
    @Scheduled(fixedRate = 30000)
    public void execute() {
        final int fileLifeTimeMinutes = 5;
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Set<String> fileNamesToRemove = new HashSet<>();

        files.forEach((key, value) -> {
            if (dateTimeNow.isAfter(value.plusMinutes(fileLifeTimeMinutes))) {
                if (new File(key).delete()) {
                    files.remove(key);
                    fileNamesToRemove.add(key);
                }
            }
        });

        deleteFiles(fileNamesToRemove);
    }

    public String addFile(String prefix, String postfix) {
        String fileName = prefix + filesCounter.incrementAndGet() + postfix;

        File file = new File(fileName);
        if (file.exists()) {
            if (!file.delete()) {
                return addFile(prefix, postfix);
            }
        }

        files.put(fileName, LocalDateTime.now());
        return fileName;
    }

    public void deleteFile(String fileName) {
        files.remove(fileName);
    }

    public void deleteFiles(Set<String> fileNames) {
        files.keySet().removeAll(fileNames);
    }

    public void deleteAllFiles() {
        for (Map.Entry<String, LocalDateTime> entry: files.entrySet()) {
            if (new File(entry.getKey()).delete()) {
                deleteFile(entry.getKey());
            }
        }

        log.warn("Failed to delete files: {}", String.join(", ", files.keySet()));
    }
}
