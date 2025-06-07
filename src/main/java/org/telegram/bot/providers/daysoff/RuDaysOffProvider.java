package org.telegram.bot.providers.daysoff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.utils.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
@Slf4j
public class RuDaysOffProvider implements DaysOffProvider {

    private static final String IS_DAY_OFF_API_URL_TEMPLATE = "https://isdayoff.ru/api/getdata?year=%s&month=%s";
    private static final String IS_DAY_OF_VALUE = "1";

    private final Map<Integer, Map<Integer, List<Integer>>> daysOffMap = new ConcurrentHashMap<>();

    private final NetworkUtils networkUtils;
    private final BotStats botStats;

    @Override
    public Locale getLocale() {
        return Locale.forLanguageTag("ru");
    }

    @Override
    public List<Integer> getDaysOffInMonth(int year, int month) {
        return daysOffMap.computeIfAbsent(year, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(month, k -> getDaysOffListFromApi(year, month));
    }

    private List<Integer> getDaysOffListFromApi(int year, int month) {
        String apiResponse;
        try {
            apiResponse = networkUtils.readStringFromURL(String.format(IS_DAY_OFF_API_URL_TEMPLATE, year, month));
        } catch (IOException e) {
            log.error("Unable to get days of", e);
            botStats.incrementErrors(year + " " + month, e, "Unable to get days of");
            return List.of();
        }

        List<Integer> daysOfList = new ArrayList<>();
        for (int i = 0; i < apiResponse.length(); i++) {
            if (IS_DAY_OF_VALUE.equals(String.valueOf(apiResponse.charAt(i)))) {
                daysOfList.add(i + 1);
            }
        }

        return daysOfList;
    }

}
