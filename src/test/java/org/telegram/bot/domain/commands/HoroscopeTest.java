package org.telegram.bot.domain.commands;

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
import org.telegram.bot.commands.Horoscope;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserZodiac;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Zodiac;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserZodiacService;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
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
        Update update = TestUtils.getUpdateFromGroup("horoscope");

        JsonParseException jsonParseException = mock(JsonParseException.class);
        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenThrow(jsonParseException);

        assertThrows((BotException.class), () -> horoscope.parse(update));

        verify(bot).sendTyping(update.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void parseUnknownHoroscopeTest() {
        final String expectedResponseText = "${command.horoscope.unknownhoroscopetype}:\n" +
                "Общий — /horoscope_com\n" +
                "Эротический — /horoscope_ero\n" +
                "Анти — /horoscope_anti\n" +
                "Бизнес — /horoscope_bus\n" +
                "Здоровье — /horoscope_hea\n" +
                "Кулинарный — /horoscope_cook\n" +
                "Любовный — /horoscope_lov\n" +
                "Мобильный — /horoscope_mob\n";
        Update update = TestUtils.getUpdateFromGroup("horoscope_abv");

        SendMessage sendMessage = horoscope.parse(update);

        verify(bot).sendTyping(update.getMessage().getChatId());
        TestUtils.checkDefaultSendMessageParams(sendMessage, ParseMode.HTML);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void parseWithoutHoroscopeTest() throws IOException {
        final String expectedResponseText = "${command.horoscope.caption} <b>Общий</b>\n" +
                "(today)\n" +
                "\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♈️Овен</a></u>aries\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♉️Телец</a></u>taurus\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♊️Близнецы</a></u>gemini\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♋️Рак</a></u>cancer\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♌️Лев</a></u>leo\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♍️Дева</a></u>virgo\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♎️Весы</a></u>libra\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♏️Скорпион</a></u>scorpio\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♐️Стрелец</a></u>sagittarius\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♑️Козерог</a></u>capricorn\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♒️Водолей</a></u>aquarius\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♓️Рыбы</a></u>pisces\n";
        Horoscope.HoroscopeData data = getHoroscopeData();
        Update update = TestUtils.getUpdateFromGroup("horoscope");

        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenReturn(data);

        SendMessage sendMessage = horoscope.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());

        TestUtils.checkDefaultSendMessageParams(sendMessage, ParseMode.HTML);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void parseKnownHoroscopeTest() throws IOException {
        final String expectedResponseText = "${command.horoscope.caption} <b>Анти</b>\n" +
                "(today)\n" +
                "\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♈️Овен</a></u>aries\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♉️Телец</a></u>taurus\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♊️Близнецы</a></u>gemini\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♋️Рак</a></u>cancer\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♌️Лев</a></u>leo\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♍️Дева</a></u>virgo\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♎️Весы</a></u>libra\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♏️Скорпион</a></u>scorpio\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♐️Стрелец</a></u>sagittarius\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♑️Козерог</a></u>capricorn\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♒️Водолей</a></u>aquarius\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♓️Рыбы</a></u>pisces\n";
        Horoscope.HoroscopeData data = getHoroscopeData();
        Update update = TestUtils.getUpdateFromGroup("horoscope_anti");

        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenReturn(data);

        SendMessage sendMessage = horoscope.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());

        TestUtils.checkDefaultSendMessageParams(sendMessage, ParseMode.HTML);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void parseKnownHoroscopeWithZodiacSetTest() throws IOException {
        final String expectedResponseText = "${command.horoscope.caption} <b>Анти</b>\n" +
                "(today)\n" +
                "<u><a href=\"https://ignio.com/r/daily/\">♒️Водолей</a></u>aquarius";
        Horoscope.HoroscopeData data = getHoroscopeData();
        Update update = TestUtils.getUpdateFromGroup("horoscope_anti");

        when(userZodiacService.get(any(Chat.class), any(User.class))).thenReturn(new UserZodiac().setZodiac(Zodiac.AQUARIUS));
        when(xmlMapper.readValue(any(File.class), ArgumentMatchers.<Class<Horoscope.HoroscopeData>>any()))
                .thenReturn(data);

        SendMessage sendMessage = horoscope.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());

        TestUtils.checkDefaultSendMessageParams(sendMessage, ParseMode.HTML);
        assertEquals(expectedResponseText, sendMessage.getText());
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

}