package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.TemporaryFileManager;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileManagerTimer extends TimerParent {

    private final TemporaryFileManager temporaryFileManager;

    @Value("${temporaryFileLifetimeSeconds:900}")
    private Integer temporaryFileLifetimeSeconds;

    @Override
    @Scheduled(fixedRate = 30000)
    public void execute() {
        temporaryFileManager.cleanup(LocalDateTime.now().plusSeconds(temporaryFileLifetimeSeconds));
    }

}
