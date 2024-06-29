package org.telegram.bot.commands;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserZodiac;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.enums.Zodiac;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserZodiacService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoroscopeTest {

    @Mock
    private Bot bot;
    @Mock
    private UserZodiacService userZodiacService;
    @Mock
    private SpeechService speechService;
    @Mock
    private XmlMapper xmlMapper;

    @InjectMocks
    private Horoscope horoscope;

    @Test
    void parseWithDeserializeExceptionTest() throws IOException {
        BotRequest request = TestUtils.getRequestFromGroup("horoscope");

        JsonParseException jsonParseException = mock(JsonParseException.class);
        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenThrow(jsonParseException);

        assertThrows((BotException.class), () -> horoscope.parse(request));

        verify(bot).sendTyping(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void parseUnknownHoroscopeTest() {
        final String expectedResponseText = """
                ${command.horoscope.unknownhoroscopetype}:
                Общий — /horoscope_com
                Эротический — /horoscope_ero
                Анти — /horoscope_anti
                Бизнес — /horoscope_bus
                Здоровье — /horoscope_hea
                Кулинарный — /horoscope_cook
                Любовный — /horoscope_lov
                Мобильный — /horoscope_mob
                """;
        BotRequest request = TestUtils.getRequestFromGroup("horoscope_abv");

        BotResponse botResponse = horoscope.parse(request).get(0);

        verify(bot).sendTyping(request.getMessage().getChatId());
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, FormattingStyle.HTML);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithoutHoroscopeTest() throws IOException {
        final String expectedResponseText = """
                ${command.horoscope.caption} <b>Общий</b>
                (today)

                <u><a href="https://ignio.com/r/daily/">♈️${enum.zodiac.aries}</a></u>aries
                <u><a href="https://ignio.com/r/daily/">♉️${enum.zodiac.taurus}</a></u>taurus
                <u><a href="https://ignio.com/r/daily/">♊️${enum.zodiac.gemini}</a></u>gemini
                <u><a href="https://ignio.com/r/daily/">♋️${enum.zodiac.cancer}</a></u>cancer
                <u><a href="https://ignio.com/r/daily/">♌️${enum.zodiac.leo}</a></u>leo
                <u><a href="https://ignio.com/r/daily/">♍️${enum.zodiac.virgo}</a></u>virgo
                <u><a href="https://ignio.com/r/daily/">♎️${enum.zodiac.libra}</a></u>libra
                <u><a href="https://ignio.com/r/daily/">♏️${enum.zodiac.scorpio}</a></u>scorpio
                <u><a href="https://ignio.com/r/daily/">♐️${enum.zodiac.sagittarius}</a></u>sagittarius
                <u><a href="https://ignio.com/r/daily/">♑️${enum.zodiac.capricorn}</a></u>capricorn
                <u><a href="https://ignio.com/r/daily/">♒️${enum.zodiac.aquarius}</a></u>aquarius
                <u><a href="https://ignio.com/r/daily/">♓️${enum.zodiac.pisces}</a></u>pisces
                """;
        Horoscope.HoroscopeData data = getHoroscopeData();
        BotRequest request = TestUtils.getRequestFromGroup("horoscope");

        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenReturn(data);

        BotResponse botResponse = horoscope.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, FormattingStyle.HTML);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithoutHoroscopeWithBigDataTest() throws IOException {
        Horoscope.HoroscopeData data = getHoroscopeBigData();
        BotRequest request = TestUtils.getRequestFromGroup("horoscope");

        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenReturn(data);

        List<BotResponse> botResponses = horoscope.parse(request);
        assertEquals(12, botResponses.size());
        BotResponse botResponse = botResponses.get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());

        TestUtils.checkDefaultTextResponseParams(botResponse, FormattingStyle.HTML);
    }

    @Test
    void parseKnownHoroscopeTest() throws IOException {
        final String expectedResponseText = """
                ${command.horoscope.caption} <b>Анти</b>
                (today)

                <u><a href="https://ignio.com/r/daily/">♈️${enum.zodiac.aries}</a></u>aries
                <u><a href="https://ignio.com/r/daily/">♉️${enum.zodiac.taurus}</a></u>taurus
                <u><a href="https://ignio.com/r/daily/">♊️${enum.zodiac.gemini}</a></u>gemini
                <u><a href="https://ignio.com/r/daily/">♋️${enum.zodiac.cancer}</a></u>cancer
                <u><a href="https://ignio.com/r/daily/">♌️${enum.zodiac.leo}</a></u>leo
                <u><a href="https://ignio.com/r/daily/">♍️${enum.zodiac.virgo}</a></u>virgo
                <u><a href="https://ignio.com/r/daily/">♎️${enum.zodiac.libra}</a></u>libra
                <u><a href="https://ignio.com/r/daily/">♏️${enum.zodiac.scorpio}</a></u>scorpio
                <u><a href="https://ignio.com/r/daily/">♐️${enum.zodiac.sagittarius}</a></u>sagittarius
                <u><a href="https://ignio.com/r/daily/">♑️${enum.zodiac.capricorn}</a></u>capricorn
                <u><a href="https://ignio.com/r/daily/">♒️${enum.zodiac.aquarius}</a></u>aquarius
                <u><a href="https://ignio.com/r/daily/">♓️${enum.zodiac.pisces}</a></u>pisces
                """;
        Horoscope.HoroscopeData data = getHoroscopeData();
        BotRequest request = TestUtils.getRequestFromGroup("horoscope_anti");

        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenReturn(data);

        BotResponse response = horoscope.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response, FormattingStyle.HTML);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseKnownHoroscopeWithZodiacSetTest() throws IOException {
        final String expectedResponseText = """
                ${command.horoscope.caption} <b>Анти</b>
                (today)
                <u><a href="https://ignio.com/r/daily/">♒️${enum.zodiac.aquarius}</a></u>aquarius""";
        Horoscope.HoroscopeData data = getHoroscopeData();
        BotRequest request = TestUtils.getRequestFromGroup("horoscope_anti");

        when(userZodiacService.get(any(Chat.class), any(User.class))).thenReturn(new UserZodiac().setZodiac(Zodiac.AQUARIUS));
        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenReturn(data);

        BotResponse botResponse = horoscope.parse(request).get(0);
        verify(bot).sendTyping(request.getMessage().getChatId());

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, FormattingStyle.HTML);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    private Horoscope.HoroscopeData getHoroscopeData() {
        return new Horoscope.HoroscopeData()
                .setDate(new Horoscope.Date()
                        .setToday("today"))
                .setAries(new Horoscope.HoroscopeElement().setToday("aries"))
                .setTaurus(new Horoscope.HoroscopeElement().setToday("taurus"))
                .setGemini(new Horoscope.HoroscopeElement().setToday("gemini"))
                .setCancer(new Horoscope.HoroscopeElement().setToday("cancer"))
                .setLeo(new Horoscope.HoroscopeElement().setToday("leo"))
                .setVirgo(new Horoscope.HoroscopeElement().setToday("virgo"))
                .setLibra(new Horoscope.HoroscopeElement().setToday("libra"))
                .setScorpio(new Horoscope.HoroscopeElement().setToday("scorpio"))
                .setSagittarius(new Horoscope.HoroscopeElement().setToday("sagittarius"))
                .setCapricorn(new Horoscope.HoroscopeElement().setToday("capricorn"))
                .setAquarius(new Horoscope.HoroscopeElement().setToday("aquarius"))
                .setPisces(new Horoscope.HoroscopeElement().setToday("pisces"));
    }

    private Horoscope.HoroscopeData getHoroscopeBigData() {
        return new Horoscope.HoroscopeData()
                .setDate(new Horoscope.Date()
                        .setToday("today"))
                .setAries(new Horoscope.HoroscopeElement().setToday("aries".repeat(500)))
                .setTaurus(new Horoscope.HoroscopeElement().setToday("taurus".repeat(500)))
                .setGemini(new Horoscope.HoroscopeElement().setToday("gemini".repeat(500)))
                .setCancer(new Horoscope.HoroscopeElement().setToday("cancer".repeat(500)))
                .setLeo(new Horoscope.HoroscopeElement().setToday("leo".repeat(500)))
                .setVirgo(new Horoscope.HoroscopeElement().setToday("virgo".repeat(500)))
                .setLibra(new Horoscope.HoroscopeElement().setToday("libra".repeat(500)))
                .setScorpio(new Horoscope.HoroscopeElement().setToday("scorpio".repeat(500)))
                .setSagittarius(new Horoscope.HoroscopeElement().setToday("sagittarius".repeat(500)))
                .setCapricorn(new Horoscope.HoroscopeElement().setToday("capricorn".repeat(500)))
                .setAquarius(new Horoscope.HoroscopeElement().setToday("aquarius".repeat(500)))
                .setPisces(new Horoscope.HoroscopeElement().setToday("pisces".repeat(500)));
    }

}