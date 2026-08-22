package org.telegram.bot.timers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class YtDlpUpdater extends TimerParent {

    @Scheduled(cron = "0 0 4 * * *")
    @Override
    public void execute() {
        if (isYtDlpInstalled()) {
            updateYtDlp();
        }
    }

    private boolean isYtDlpInstalled() {
        try {
            Process process = new ProcessBuilder("yt-dlp", "--version")
                    .redirectErrorStream(true)
                    .start();

            int exitCode = process.waitFor();

            return exitCode == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private void updateYtDlp() {
        try {
            Process process = new ProcessBuilder("yt-dlp", "-U")
                    .redirectErrorStream(true)
                    .start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Failed to upgrade yt-dlp, exit code: {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("yt-dlp update was interrupted");
        } catch (IOException e) {
            log.error("Failed to execute yt-dlp update", e);
        }
    }
}