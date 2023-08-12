package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.telegram.bot.utils.TextUtils.startsWithNumber;
import static org.telegram.bot.utils.TextUtils.parseFloat;

@Component
@RequiredArgsConstructor
@Slf4j
public class Exchange implements CommandParent<SendMessage> {

    private static final Pattern VALUTE_TO_RUB_PATTERN = Pattern.compile("^(\\d+[.,]*\\d*)\\s?([a-zA-Zа-яА-Я]+)$");
    private static final Pattern RUB_TO_VALUTE_PATTERN = Pattern.compile("^(\\d+[.,]*\\d*)\\s?([a-zA-Zа-яА-Я]+)\\.?\\s?([a-zA-Zа-яА-Я]+)$");

    private final Bot bot;
    private final SpeechService speechService;
    private final CommandPropertiesService commandPropertiesService;
    private final NetworkUtils networkUtils;
    private final XmlMapper xmlMapper;
    private final Clock clock;
    private final Map<LocalDate, ValCurs> valCursDataMap = new ConcurrentHashMap<>();

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        if (textMessage == null) {
            log.debug("Request to get exchange rates for usd and eur");
            responseText = getExchangeRatesForUsdAndEur();
        } else {
            if (startsWithNumber(textMessage)) {
                Matcher valuteToRubMatcher = VALUTE_TO_RUB_PATTERN.matcher(textMessage);
                Matcher ruToValuteMatcher = RUB_TO_VALUTE_PATTERN.matcher(textMessage);

                if (valuteToRubMatcher.find()) {
                    String currencyCode = valuteToRubMatcher.group(2);
                    float amount =  Float.parseFloat(valuteToRubMatcher.group(1));
                    log.debug("Request to get rubles count for currency {} amount {}", currencyCode, amount);
                    responseText = getRublesForCurrencyValue(currencyCode, amount);
                } else if (ruToValuteMatcher.find()) {
                    String valuteCode = ruToValuteMatcher.group(3);
                    float rublesAmount =  Float.parseFloat(ruToValuteMatcher.group(1));
                    log.debug("Request to get valute {} count for rubles amount {}", valuteCode, rublesAmount);
                    responseText = getValuteForRublesAmount(valuteCode, rublesAmount);
                } else {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            } else {
                if (textMessage.startsWith("_")) {
                    textMessage = textMessage.substring(1);
                }

                log.debug("Request to get exchange rates for {}", textMessage);
                responseText = getExchangeRatesForCode(textMessage.toUpperCase(Locale.ROOT));
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Getting exchange rates for USD and EUR.
     *
     * @return formatted text with exchange rates.
     */
    private String getExchangeRatesForUsdAndEur() {
        ValCurs valCursCurrent = getValCursData();
        String cursDate = valCursCurrent.getDate();
        ValCurs valCursBefore = getValCursData(LocalDate.parse(cursDate, DateUtils.dateFormatter).minusDays(1));

        List<Valute> valuteList = valCursCurrent.getValute();
        List<Valute> valuteListYesterday = valCursBefore.getValute();

        Float usdCurrent = getValuteByCode(valuteList, "USD").getValuteValue();
        Float usdBefore = getValuteByCode(valuteListYesterday, "USD").getValuteValue();
        Float eurCurrent = getValuteByCode(valuteList, "EUR").getValuteValue();
        Float eurBefore = getValuteByCode(valuteListYesterday, "EUR").getValuteValue();

        return "<b>Курс валют ЦБ РФ:</b>\n" +
                "$ USD = " + formatFloatValue(usdCurrent) + " RUB " + getDynamicsForValuteValue(usdCurrent, usdBefore) + "\n" +
                "€ EUR = " + formatFloatValue(eurCurrent) + " RUB " + getDynamicsForValuteValue(eurCurrent, eurBefore) + "\n" +
                "(" + cursDate + ")";
    }

    /**
     * Getting rubles count for currency amount.
     *
     * @return formatted text with rubles count.
     */
    private String getRublesForCurrencyValue(String code, Float amount) {
        List<Valute> valuteList = getValCursData().getValute();

        Valute valute = getValuteByCode(valuteList, code);
        if (valute == null) {
            return "Не нашёл валюту <b>" + code + "</b>\nСписок доступных: " + getValuteList(valuteList);
        } else {
            float exchangeRate = getReversExchangeRate(valute);
            return "<b>" + valute.getName() + " в Рубли</b>\n" +
                    String.valueOf(amount).replaceAll("\\.", ",") + " " + valute.getCharCode() + " = " + formatFloatValue(amount / exchangeRate) + " ₽";
        }
    }

    /**
     * Getting currency amont for rubles
     *
     * @param valuteCode code of valute.
     * @param amount amount of rubles.
     * @return formatted text with valute count.
     */
    private String getValuteForRublesAmount(String valuteCode, Float amount) {
        List<Valute> valuteList = getValCursData().getValute();

        Valute valute = getValuteByCode(valuteList, valuteCode);
        if (valute == null) {
            return "Не нашёл валюту <b>" + valuteCode + "</b>\nСписок доступных: " + getValuteList(valuteList);
        } else {
            float exchangeRate = getReversExchangeRate(valute);
            return "<b>" + "Рубли в " + valute.getName() + "</b>\n" +
                    String.valueOf(amount).replaceAll("\\.", ",") + " ₽ = " + formatFloatValue(amount * exchangeRate) + " " + valute.getCharCode();
        }
    }

    /**
     * Getting exchange rates for a specific valute.
     *
     * @param code code of the valute.
     * @return formatted text with exchange rates.
     */
    private String getExchangeRatesForCode(String code) {
        ValCurs valCursCurrent = getValCursData();
        String cursDate = valCursCurrent.getDate();
        ValCurs valCursBefore = getValCursData(LocalDate.parse(cursDate, DateUtils.dateFormatter).minusDays(1));

        List<Valute> valuteList = valCursCurrent.getValute();
        List<Valute> valuteListYesterday = valCursBefore.getValute();

        Valute valute = getValuteByCode(valuteList, code);
        if (valute == null) {
            return "Не нашёл валюту <b>" + code + "</b>\nСписок доступных: " + getValuteList(valuteList);
        } else {
            Float valuteValueBefore = getValuteByCode(valuteListYesterday, code).getValuteValue();
            String dynamicsForValuteValue = getDynamicsForValuteValue(valute.getValuteValue(), valuteValueBefore);
            String reversExchangeRate = formatFloatValue(getReversExchangeRate(valute));

            return "<b>" + valute.getName() + "</b>\n" +
                    valute.getNominal() + " " + valute.getCharCode() + " = " + valute.getValue() + " RUB " + dynamicsForValuteValue + "\n" +
                   "1 RUB = " + reversExchangeRate + " " + valute.getCharCode() + "\n" +
                    "(" + cursDate + ")";
        }
    }

    private float getReversExchangeRate(Valute valute) {
        return parseFloat(valute.getNominal()) / valute.getValuteValue();
    }

    /**
     * Getting currency dynamics.
     *
     * @param valuteCurrent current value of Valute.
     * @param valuteBefore yesterday value of Valute.
     * @return emoji.
     */
    private String getDynamicsForValuteValue(Float valuteCurrent, Float valuteBefore) {
        int compareResult = valuteCurrent.compareTo(valuteBefore);

        String emoji;
        if (compareResult > 0) {
            emoji = Emoji.UP_ARROW.getEmoji();
        } else if (compareResult < 0) {
            emoji = Emoji.DOWN_ARROW.getEmoji();
        } else {
            emoji = "";
        }

        return emoji + " (" + formatFloatValueWithLeadingSing(valuteCurrent - valuteBefore) + ")";
    }

    /**
     * Getting getting a list of available valutes.
     *
     * @param valuteList list with data of exchange rates.
     * @return formatted text with list of valutes.
     */
    private String getValuteList(List<Valute> valuteList) {
        StringBuilder buf = new StringBuilder();
        String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();

        valuteList
                .forEach(valute -> buf
                        .append(valute.getName()).append(" - /").append(commandName).append("_").append(valute.getCharCode().toLowerCase(Locale.ROOT)).append("\n"));

        return buf.toString();
    }

    /**
     * Getting Valute from list by code.
     *
     * @param valuteList list with data of exchange rates.
     * @param code code of the valute.
     * @return formatted text with list of valutes.
     */
    private Valute getValuteByCode(List<Valute> valuteList, String code) {
        return valuteList
                .stream()
                .filter(valute -> valute.getCharCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * Getting current exchange rates data from service.
     *
     * @return list with data of exchange rates.
     */
    private ValCurs getValCursData() {
        return getValCursData(LocalDate.now(clock));
    }

    /**
     * Getting exchange rates data.
     *
     * @param date exchange date.
     * @return list with data of exchange rates.
     */
    private ValCurs getValCursData(LocalDate date) {
        if (valCursDataMap.containsKey(date)) {
            return valCursDataMap.get(date);
        }

        LocalDate expiredDate = LocalDate.now(clock).minusDays(2);
        for (LocalDate key: valCursDataMap.keySet()) {
            if (key.isBefore(expiredDate)) {
                valCursDataMap.remove(key);
            }
        }

        ValCurs valCurs = getValCursDataFromApi(date);
        valCursDataMap.put(date, valCurs);

        return valCurs;
    }

    /**
     * Getting exchange rates data from API.
     *
     * @param date exchange date.
     * @return list with data of exchange rates.
     */
    private ValCurs getValCursDataFromApi(@Nullable LocalDate date) {
        String xmlUrl = "http://www.cbr.ru/scripts/XML_daily.asp";
        if (date != null) {
            xmlUrl = xmlUrl + "?date_req=" + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date);
        }

        String response;

        try {
            response = networkUtils.readStringFromURL(xmlUrl, Charset.forName("windows-1251"));
        } catch (IOException e) {
            log.error("Error from CBRF api:", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        ValCurs valCurs;
        try {
            valCurs = xmlMapper.readValue(response, ValCurs.class);
        } catch (JsonProcessingException e) {
            log.error("Error while mapping response:", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return valCurs;
    }

    private String formatFloatValue(float value) {
        return formatFloat("%.4f", value);
    }

    private String formatFloatValueWithLeadingSing(float value) {
        return formatFloat("%+.4f", value);
    }

    private String formatFloat(String format, float value) {
        return String.format(format, value);
    }

    @Data
    public static class ValCurs {
        @XmlAttribute
        private String name;

        @XmlAttribute(name = "Date")
        private String date;

        @XmlElement(name = "Valute")
        private List<Valute> valute;
    }

    @Data
    public static class Valute {
        @XmlElement(name = "CharCode")
        private String charCode;

        @XmlElement(name = "Value")
        private String value;

        @XmlElement(name = "ID")
        private String id;

        @XmlElement(name = "Nominal")
        private String nominal;

        @XmlElement(name = "NumCode")
        private String numCode;

        @XmlElement(name = "Name")
        private String name;

        public Float getValuteValue() {
            return parseFloat(this.value);
        }
    }
}
