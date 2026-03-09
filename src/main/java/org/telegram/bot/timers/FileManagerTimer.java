package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.TemporaryFileManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileManagerTimer extends TimerParent {

    private final TemporaryFileManager temporaryFileManager;

    @Override
    @Scheduled(fixedRate = 30000)
    public void execute() {
        temporaryFileManager.cleanup();
    }

}
