package org.telegram.bot.providers.daysoff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.utils.NetworkUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuDaysOffProviderTest {

    private final Map<Integer, Map<Integer, List<Integer>>> daysOffMap = new HashMap<>();

    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private RuDaysOffProvider daysOffProvider;

    @BeforeEach
    public void init() {
        daysOffMap.clear();
        ReflectionTestUtils.setField(daysOffProvider, "daysOffMap", daysOffMap);
    }

    @Test
    void getLocaleTest() {
        final Locale expectedLocale = Locale.forLanguageTag("ru");
        Locale actualLocale = daysOffProvider.getLocale();
        assertEquals(expectedLocale, actualLocale);
    }

    @Test
    void getDaysOffInMonthWithNoApiResponseTest() throws IOException {
        when(networkUtils.readStringFromURL(anyString())).thenThrow(new IOException());

        List<Integer> daysOffInMonth = daysOffProvider.getDaysOffInMonth(2000, 1);

        assertTrue(daysOffInMonth.isEmpty());
        verify(botStats).incrementErrors(anyString(), any(Throwable.class), anyString());
    }

    @Test
    void getDaysOffInMonth() throws IOException {
        final List<Integer> expectedList = List.of(4, 5, 6, 11, 12, 13, 14, 15);

        when(networkUtils.readStringFromURL(anyString())).thenReturn("000111000011111");

        List<Integer> actualList = daysOffProvider.getDaysOffInMonth(2000, 1);

        assertEquals(expectedList, actualList);
    }

}