package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.domain.enums.Horoscope;
import org.telegram.bot.services.TimerService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoroscopeTimer extends TimerParent  {

    private final TimerService timerService;

    @Override
    @Scheduled(fixedRate = 14400000)
    public void execute() {
        Timer timer = timerService.get("horoscopeDownloader");
        if (timer == null) {
            timer = new Timer()
                    .setName("horoscopeDownloader")
                    .setLastAlarmDt(LocalDateTime.now().minusDays(1));
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            final String horoscopeDataUrl = "https://ignio.com/r/export/utf/xml/daily/";

            Arrays.stream(Horoscope.values()).forEach(horoscope -> {
                String horoscopeName = horoscope.name().toLowerCase(Locale.ROOT) + ".xml";
                try {
                    FileUtils.copyURLToFile(new URL(horoscopeDataUrl + horoscopeName), new File("horoscope/" + horoscopeName));
                } catch (IOException e) {
                    log.error("Failed to download horoscope {}: {}", horoscopeName, e.getMessage());
                }
            });
        }

        timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
        timerService.save(timer);
    }
}
