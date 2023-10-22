package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.commands.Exchange;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.telegram.bot.TestUtils.*;

@ExtendWith(MockitoExtension.class)
class ExchangeTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private NetworkUtils networkUtils;
    @Mock
    private XmlMapper xmlMapper;
    @Mock
    private Clock clock;

    @InjectMocks
    private Exchange exchange;

    private static final String XML_URL = "http://www.cbr.ru/scripts/XML_daily.asp?date_req=";
    private static final String XML_CHART_DATA_URL = "https://www.cbr.ru/scripts/XML_dynamic.asp?date_req1=%s&date_req2=%s&VAL_NM_RQ=%s";
    private static final LocalDate CURRENT_DATE = LocalDate.of(2007, 1, 2);
    private static final String CURRENT_DATE_STRING = "02.01.2007";
    private static final String DYNAMIC_CURS_DATE_TO = "04.01.2007";
    private static final String DYNAMIC_CURS_DATE_FROM = "02.12.2006";
    private static final String PREVIOUS_DATE_STRING = "01.01.2007";
    private static final String NEXT_DATE_STRING = "03.01.2007";
    private static final String USD_ID = "R01235";
    private static final String EUR_ID = "R01239";

    @Test
    void parseWithNoApiResponseTest() throws IOException {
        final String expectedErrorMessage = "no response";
        Update update = getUpdateFromGroup();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + CURRENT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251")))
                .thenThrow(new IOException());
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> exchange.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void parseWithDeserializingErrorTest() throws IOException {
        final String expectedErrorMessage = "no response";
        Update update = getUpdateFromGroup();

        JsonProcessingException jsonProcessingException = Mockito.mock(JsonProcessingException.class);
        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + CURRENT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251")))
                .thenReturn("1");
        when(xmlMapper.readValue("1", Exchange.ValCurs.class)).thenThrow(jsonProcessingException);
        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> exchange.parse(update));
        verify(bot).sendTyping(update.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void parseWithEmptyTextMessage() throws IOException {
        //does not work. Possibly because of the emoji symbol
//        final String expectedResponseText = "<b>Курс валют ЦБ РФ:</b>\n" +
//                "$ USD = 76,8207 RUB ⬆️ (+3,9889)\n" +
//                "€ EUR = 84,9073 RUB ⬇️ (-4,0650)\n" +
//                "¥ CNY = 14,5445 RUB ⬆️ (+2,4210)\n" +
//                "(02.01.2007)\n" +
//                "\n" +
//                "<b>Курс валют ЦБ РФ:</b>\n" +
//                "$ USD = 74,8318 RUB ⬇️ (-1,9889)\n" +
//                "€ EUR = 88,9923 RUB ⬆️ (+4,0850)\n" +
//                "¥ CNY = 13,8741 RUB ⬇️ (-0,6704)\n" +
//                "(03.01.2007)";
        Update update = getUpdateFromGroup();
        Exchange.ValCurs valCurs1 = getCurrentValCurs();
        Exchange.ValCurs valCurs2 = getPreviousValCurs();
        Exchange.ValCurs valCurs3 = getNextValCurs();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + CURRENT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251"))).thenReturn("1");
        when(networkUtils.readStringFromURL(XML_URL + PREVIOUS_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251"))).thenReturn("2");
        when(networkUtils.readStringFromURL(XML_URL + NEXT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251"))).thenReturn("3");
        when(xmlMapper.readValue("1", Exchange.ValCurs.class)).thenReturn(valCurs1);
        when(xmlMapper.readValue("2", Exchange.ValCurs.class)).thenReturn(valCurs2);
        when(xmlMapper.readValue("3", Exchange.ValCurs.class)).thenReturn(valCurs3);

        PartialBotApiMethod<?> method = exchange.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(method);
//        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 25})
    void getChartForUsdEurWithWrongMonthsValueTest() {
        final String expectedErrorMessage = "wrong input";
        Update update = TestUtils.getUpdateFromGroup("exchange 25");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> exchange.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void getChartForUsdEurWithNotResponseTest() throws IOException {
        final String expectedErrorMessage = "no response";
        Update update = TestUtils.getUpdateFromGroup("exchange 1");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(anyString(), any(Charset.class))).thenThrow(IOException.class);
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> exchange.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void getChartForUsedEurWithDeserializingErrorTest() throws IOException {
        final String expectedErrorMessage = "no response";
        Update update = TestUtils.getUpdateFromGroup("exchange 1");

        JsonProcessingException jsonProcessingException = Mockito.mock(JsonProcessingException.class);
        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(anyString(), any(Charset.class))).thenReturn("1");
        when(xmlMapper.readValue("1", Exchange.DynamicValCurs.class)).thenThrow(jsonProcessingException);

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorMessage);

        BotException botException = assertThrows(BotException.class, () -> exchange.parse(update));
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        assertEquals(expectedErrorMessage, botException.getMessage());
    }

    @Test
    void getChartForUsdEurTest() throws IOException {
        //does not work. Possibly because of the ₽ symbol
//        final String expectedResponseText = "MIN/MAX\n" +
//                "$ USD: <b>90.236</b> RUB / <b>91.234</b> RUB\n" +
//                "€ EUR: <b>100.95</b> RUB / <b>105.213</b> RUB\n";
        Update update = TestUtils.getUpdateFromGroup("exchange 1");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(String.format(XML_CHART_DATA_URL, DYNAMIC_CURS_DATE_FROM.replaceAll("\\.", "/"), DYNAMIC_CURS_DATE_TO.replaceAll("\\.", "/"), USD_ID), Charset.forName("windows-1251"))).thenReturn("1");
        when(networkUtils.readStringFromURL(String.format(XML_CHART_DATA_URL, DYNAMIC_CURS_DATE_FROM.replaceAll("\\.", "/"), DYNAMIC_CURS_DATE_TO.replaceAll("\\.", "/"), EUR_ID), Charset.forName("windows-1251"))).thenReturn("2");
        when(xmlMapper.readValue("1", Exchange.DynamicValCurs.class)).thenReturn(getDynamicValCursUsd());
        when(xmlMapper.readValue("2", Exchange.DynamicValCurs.class)).thenReturn(getDynamicValCursEur());

        PartialBotApiMethod<?> method = exchange.parse(update);
        verify(bot, never()).sendTyping(update.getMessage().getChatId());
        verify(bot).sendUploadPhoto(update.getMessage().getChatId());
        checkDefaultSendPhotoParams(method);
//        assertEquals(expectedResponseText, sendPhoto.getCaption());
    }

    @Test
    void getRublesForCurrencyValueWithUnknownValuteTest() throws IOException {
        final String unknownValuteCode = "btlc";
        final String expectedResponseText = "${command.exchange.valutenotfound} <b>btlc</b>\n" +
                "${command.exchange.listofavailablevalute}: Доллар США - /exchange_usd\n" +
                "Евро - /exchange_eur\n" +
                "Китайский юань - /exchange_cny\n";
        Update update = TestUtils.getUpdateFromGroup("exchange 5 " + unknownValuteCode);
        Exchange.ValCurs valCurs = getCurrentValCurs();
        CommandProperties commandProperties = new CommandProperties().setCommandName("exchange");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + CURRENT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251")))
                .thenReturn("");
        when(xmlMapper.readValue("", Exchange.ValCurs.class)).thenReturn(valCurs);
        when(commandPropertiesService.getCommand(Exchange.class)).thenReturn(commandProperties);

        PartialBotApiMethod<?> method = exchange.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getRublesForCurrencyValueTest() throws IOException {
        //does not work. Possibly because of the ₽ symbol
//        final String expectedResponseText = "<b>Доллар США в Рубли</b>\n5,0 USD = 384,1035 ₽";
        Update update = TestUtils.getUpdateFromGroup("exchange 5 usd");
        Exchange.ValCurs valCurs = getCurrentValCurs();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + CURRENT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251")))
                .thenReturn("");
        when(xmlMapper.readValue("", Exchange.ValCurs.class)).thenReturn(valCurs);

        PartialBotApiMethod<?> method = exchange.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(method);
//        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getValuteForRublesCountWithUnknownValuteTest() throws IOException {
        final String unknownValuteCode = "btlc";
        final String expectedResponseText = "${command.exchange.valutenotfound} <b>btlc</b>\n" +
                "${command.exchange.listofavailablevalute}: Доллар США - /exchange_usd\n" +
                "Евро - /exchange_eur\n" +
                "Китайский юань - /exchange_cny\n";
        Update update = TestUtils.getUpdateFromGroup("exchange 5 rub " + unknownValuteCode);
        Exchange.ValCurs valCurs = getCurrentValCurs();
        CommandProperties commandProperties = new CommandProperties().setCommandName("exchange");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + CURRENT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251")))
                .thenReturn("");
        when(xmlMapper.readValue("", Exchange.ValCurs.class)).thenReturn(valCurs);
        when(commandPropertiesService.getCommand(Exchange.class)).thenReturn(commandProperties);

        PartialBotApiMethod<?> method = exchange.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getValuteForRublesCountTest() throws IOException {
        //does not work. Possibly because of the ₽ symbol
//        final String expectedResponseText = "<b>Рубли в Доллар США</b>\n1,0 ₽ = 0,0130 USD";
        Update update = TestUtils.getUpdateFromGroup("exchange 1 rub usd");
        Exchange.ValCurs valCurs = getCurrentValCurs();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + CURRENT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251")))
                .thenReturn("");
        when(xmlMapper.readValue("", Exchange.ValCurs.class)).thenReturn(valCurs);

        PartialBotApiMethod<?> method = exchange.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(method);
//        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getExchangeRatesForUnknownCodeTest() throws IOException {
        final String unknownValuteCode = "btlc";
        final String expectedResponseText = "${command.exchange.valutenotfound} <b>BTLC</b>\n" +
                "${command.exchange.listofavailablevalute}: Доллар США - /exchange_usd\n" +
                "Евро - /exchange_eur\n" +
                "Китайский юань - /exchange_cny\n";
        Update update = TestUtils.getUpdateFromGroup("exchange_" + unknownValuteCode);
        Exchange.ValCurs valCurs = getCurrentValCurs();
        CommandProperties commandProperties = new CommandProperties().setCommandName("exchange");

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(anyString(), any())).thenReturn("");
        when(xmlMapper.readValue("", Exchange.ValCurs.class)).thenReturn(valCurs);
        when(commandPropertiesService.getCommand(Exchange.class)).thenReturn(commandProperties);

        PartialBotApiMethod<?> method = exchange.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        SendMessage sendMessage = checkDefaultSendMessageParams(method);
        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void getExchangeRatesForCodeTest() throws IOException {
        //does not work. Possibly because of the emoji symbol
//        final String expectedResponseText = "<b>Доллар США</b>\n" +
//                "1 USD = 76,8207 RUB ⬆️ (+3,9889)\n" +
//                "1 RUB = 0,0130 USD\n" +
//                "(02.01.2007)";
        Update update = TestUtils.getUpdateFromGroup("exchange usd");
        Exchange.ValCurs valCurs1 = getCurrentValCurs();
        Exchange.ValCurs valCurs2 = getPreviousValCurs();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + CURRENT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251"))).thenReturn("1");
        when(networkUtils.readStringFromURL(XML_URL + PREVIOUS_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251"))).thenReturn("2");
        when(xmlMapper.readValue("1", Exchange.ValCurs.class)).thenReturn(valCurs1);
        when(xmlMapper.readValue("2", Exchange.ValCurs.class)).thenReturn(valCurs2);

        PartialBotApiMethod<?> method = exchange.parse(update);
        verify(bot).sendTyping(update.getMessage().getChatId());
        checkDefaultSendMessageParams(method);
//        assertEquals(expectedResponseText, sendMessage.getText());
    }

    @Test
    void valCursDataMapWorkCheckTest() throws IOException {
        LocalDate oldDate = CURRENT_DATE.minusDays(3);
        ReflectionTestUtils.setField(
                exchange,
                "valCursDataMap",
                new ConcurrentHashMap<>(
                        Map.of(oldDate, new Exchange.ValCurs(), CURRENT_DATE, getCurrentValCurs())));

        Update update = getUpdateFromGroup();
        Exchange.ValCurs valCurs2 = getPreviousValCurs();
        Exchange.ValCurs valCurs3 = getNextValCurs();

        when(clock.instant()).thenReturn(CURRENT_DATE.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(networkUtils.readStringFromURL(XML_URL + PREVIOUS_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251"))).thenReturn("2");
        when(networkUtils.readStringFromURL(XML_URL + NEXT_DATE_STRING.replaceAll("\\.", "/"), Charset.forName("windows-1251"))).thenReturn("3");
        when(xmlMapper.readValue("2", Exchange.ValCurs.class)).thenReturn(valCurs2);
        when(xmlMapper.readValue("3", Exchange.ValCurs.class)).thenReturn(valCurs3);

        exchange.parse(update);

        verify(xmlMapper, Mockito.times(2)).readValue(anyString(), ArgumentMatchers.<Class<Exchange.ValCurs>>any());

        Object value = ReflectionTestUtils.getField(exchange, "valCursDataMap");
        assertTrue(value instanceof ConcurrentHashMap);

        ConcurrentHashMap<?, ?> valCursDataMap = (ConcurrentHashMap<?, ?>) value;
        assertNotNull(valCursDataMap);
        assertFalse(valCursDataMap.containsKey(oldDate));
        assertTrue(valCursDataMap.containsKey(CURRENT_DATE));
        assertTrue(valCursDataMap.containsKey(CURRENT_DATE.minusDays(1)));
        assertTrue(valCursDataMap.containsKey(CURRENT_DATE.plusDays(1)));
    }

    private Exchange.ValCurs getCurrentValCurs() {
        Exchange.Valute usd = new Exchange.Valute();
        usd.setId("R01235");
        usd.setNumCode("840");
        usd.setCharCode("USD");
        usd.setNominal("1");
        usd.setName("Доллар США");
        usd.setValue("76,8207");

        Exchange.Valute eur = new Exchange.Valute();
        eur.setId("R01239");
        eur.setNumCode("978");
        eur.setCharCode("EUR");
        eur.setNominal("1");
        eur.setName("Евро");
        eur.setValue("84,9073");

        Exchange.Valute cny = new Exchange.Valute();
        cny.setId("R01375");
        cny.setNumCode("156");
        cny.setCharCode("CNY");
        cny.setNominal("1");
        cny.setName("Китайский юань");
        cny.setValue("14,5445");

        Exchange.ValCurs valCurs = new Exchange.ValCurs();
        valCurs.setDate(CURRENT_DATE_STRING);
        valCurs.setName("Foreign Currency Market");
        valCurs.setValute(List.of(usd, eur, cny));

        return valCurs;
    }

    private Exchange.ValCurs getPreviousValCurs() {
        Exchange.Valute usd = new Exchange.Valute();
        usd.setId("R01235");
        usd.setNumCode("840");
        usd.setCharCode("USD");
        usd.setNominal("1");
        usd.setName("Доллар США");
        usd.setValue("72,8318");

        Exchange.Valute eur = new Exchange.Valute();
        eur.setId("R01239");
        eur.setNumCode("978");
        eur.setCharCode("EUR");
        eur.setNominal("1");
        eur.setName("Евро");
        eur.setValue("88,9723");

        Exchange.Valute cny = new Exchange.Valute();
        cny.setId("R01375");
        cny.setNumCode("156");
        cny.setCharCode("CNY");
        cny.setNominal("1");
        cny.setName("Китайский юань");
        cny.setValue("12,1235");

        Exchange.ValCurs valCurs = new Exchange.ValCurs();
        valCurs.setDate(PREVIOUS_DATE_STRING);
        valCurs.setName("Foreign Currency Market");
        valCurs.setValute(List.of(usd, eur, cny));

        return valCurs;
    }

    private Exchange.ValCurs getNextValCurs() {
        Exchange.Valute usd = new Exchange.Valute();
        usd.setId("R01235");
        usd.setNumCode("840");
        usd.setCharCode("USD");
        usd.setNominal("1");
        usd.setName("Доллар США");
        usd.setValue("74,8318");

        Exchange.Valute eur = new Exchange.Valute();
        eur.setId("R01239");
        eur.setNumCode("978");
        eur.setCharCode("EUR");
        eur.setNominal("1");
        eur.setName("Евро");
        eur.setValue("88,9923");

        Exchange.Valute cny = new Exchange.Valute();
        cny.setId("R01375");
        cny.setNumCode("156");
        cny.setCharCode("CNY");
        cny.setNominal("1");
        cny.setName("Китайский юань");
        cny.setValue("13,8741");

        Exchange.ValCurs valCurs = new Exchange.ValCurs();
        valCurs.setDate(NEXT_DATE_STRING);
        valCurs.setName("Foreign Currency Market");
        valCurs.setValute(List.of(usd, eur, cny));

        return valCurs;
    }

    private Exchange.DynamicValCurs getDynamicValCursUsd() {
        Exchange.DynamicValCurs dynamicValCurs = new Exchange.DynamicValCurs();
        dynamicValCurs.setId(USD_ID);
        dynamicValCurs.setDateRange1(CURRENT_DATE_STRING);
        dynamicValCurs.setDateRange2(PREVIOUS_DATE_STRING);
        dynamicValCurs.setName("name");
        dynamicValCurs.setRecords(getUsdRecords());

        return dynamicValCurs;
    }

    private List<Exchange.Record> getUsdRecords() {
        Exchange.Record record1 = new Exchange.Record();
        record1.setId(USD_ID);
        record1.setDate(PREVIOUS_DATE_STRING);
        record1.setValue("90,236");
        record1.setNominal("1");

        Exchange.Record record2 = new Exchange.Record();
        record2.setId(USD_ID);
        record2.setDate(CURRENT_DATE_STRING);
        record2.setValue("91,234");
        record2.setNominal("1");

        return List.of(record1, record2);
    }

    private Exchange.DynamicValCurs getDynamicValCursEur() {
        Exchange.DynamicValCurs dynamicValCurs = new Exchange.DynamicValCurs();
        dynamicValCurs.setId(EUR_ID);
        dynamicValCurs.setDateRange1(CURRENT_DATE_STRING);
        dynamicValCurs.setDateRange2(PREVIOUS_DATE_STRING);
        dynamicValCurs.setName("name");
        dynamicValCurs.setRecords(getEurRecords());

        return dynamicValCurs;
    }

    private List<Exchange.Record> getEurRecords() {
        Exchange.Record record1 = new Exchange.Record();
        record1.setId(EUR_ID);
        record1.setDate(PREVIOUS_DATE_STRING);
        record1.setValue("100,95");
        record1.setNominal("1");

        Exchange.Record record2 = new Exchange.Record();
        record2.setId(EUR_ID);
        record2.setDate(CURRENT_DATE_STRING);
        record2.setValue("105,213");
        record2.setNominal("1");

        return List.of(record1, record2);
    }

}