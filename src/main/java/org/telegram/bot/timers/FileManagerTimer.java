package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
        final int fileLifeTimeMinutes = 10;
        LocalDateTime dateTimeNow = LocalDateTime.now();

        for (Map.Entry<String, LocalDateTime> entry: files.entrySet()) {
            if (dateTimeNow.isAfter(entry.getValue().plusMinutes(fileLifeTimeMinutes))) {
                if (new File(entry.getKey()).delete()) {
                    files.remove(entry.getKey());
                }
            }
        }
    }

    public String addFile(String prefix, String postfix) {
        String fileName = prefix + filesCounter.incrementAndGet() + postfix;
        files.put(fileName, LocalDateTime.now());
        return fileName;
    }
}
