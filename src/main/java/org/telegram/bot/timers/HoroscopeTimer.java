package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.enums.Horoscope;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoroscopeTimer extends TimerParent  {

    private static final String HOROSCOPE_DATA_URL = "https://ignio.com/r/export/utf/xml/daily/";

    @Override
    @Scheduled(cron = "0 5 0 * * ?")
    public void execute() {
        Arrays.stream(Horoscope.values()).forEach(horoscope -> {
            String horoscopeName = horoscope.name().toLowerCase(Locale.ROOT) + ".xml";
            try {
                FileUtils.copyURLToFile(new URL(HOROSCOPE_DATA_URL + horoscopeName), new File("horoscope/" + horoscopeName));
            } catch (IOException e) {
                log.error("Failed to download horoscope {}: {}", horoscopeName, e.getMessage());
            }
        });
    }
}
