package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class Exchange implements CommandParent<SendMessage> {

    private final SpeechService speechService;
    private final CommandPropertiesService commandPropertiesService;
    private final NetworkUtils networkUtils;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        ValCurs valCursCurrent = getValCursData();
        String cursDate = valCursCurrent.getDate();
        ValCurs valCursBefore = getValCursData(LocalDate.parse(cursDate, DateUtils.dateFormatter).minusDays(1));

        List<Valute> valuteList = valCursCurrent.getValute();
        List<Valute> valuteListYesterday = valCursBefore.getValute();

        if (textMessage == null) {
            log.debug("Request to get exchange rates for usd and eur");
            responseText = getExchangeRatesForUsdAndEur(valuteList, valuteListYesterday, cursDate);
        } else {
            if (textMessage.startsWith("_")) {
                textMessage = textMessage.substring(1);
            }

            log.debug("Request to get exchange rates for {}", textMessage);
            responseText = getExchangeRatesForCode(valuteList, valuteListYesterday, textMessage.toUpperCase(Locale.ROOT), cursDate);
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
     * @param valuteList list with data of exchange rates.
     * @param date date of current exchange rates.
     * @param valuteListYesterday exchange rate for yesterday.
     * @return formatted text with exchange rates.
     */
    private String getExchangeRatesForUsdAndEur(List<Valute> valuteList, List<Valute> valuteListYesterday, String date) {
        BigDecimal usdCurrent = getValuteByCode(valuteList, "USD").getValuteValue();
        BigDecimal usdBefore = getValuteByCode(valuteListYesterday, "USD").getValuteValue();
        BigDecimal eurCurrent = getValuteByCode(valuteList, "EUR").getValuteValue();
        BigDecimal eurBefore = getValuteByCode(valuteListYesterday, "EUR").getValuteValue();

        return "<b>Курс валют ЦБ РФ:</b>\n" +
                "$ USD = " + usdCurrent + " RUB " + getDynamicEmojiForValuteValue(usdCurrent, usdBefore) + "\n" +
                "€ EUR = " + eurCurrent + " RUB " + getDynamicEmojiForValuteValue(eurCurrent, eurBefore) + "\n" +
                "(" + date + ")";
    }

    /**
     * Getting exchange rates for a specific valute.
     *
     * @param valuteList list with data of exchange rates.
     * @param valuteListYesterday exchange rate for yesterday.
     * @param code code of the valute.
     * @return formatted text with exchange rates.
     */
    private String getExchangeRatesForCode(List<Valute> valuteList, List<Valute> valuteListYesterday, String code, String date) {
        Valute valute = getValuteByCode(valuteList, code);
        if (valute == null) {
            return "Не нашёл валюту <b>" + code + "</b>\nСписок доступных: " + getValuteList(valuteList);
        } else {
            BigDecimal valuteValueBefore = getValuteByCode(valuteListYesterday, code).getValuteValue();

            return "<b>" + valute.getName() + "</b>\n" +
                    valute.getNominal() + " " + valute.getCharCode() + " = " + valute.getValue() + " RUB " + getDynamicEmojiForValuteValue(valute.getValuteValue(), valuteValueBefore) + "\n" +
                   "1 RUB = " + Float.parseFloat(valute.getNominal().replaceAll(",", ".")) / Float.parseFloat(valute.getValue().replaceAll(",", ".")) + " " + valute.getCharCode() + "\n" +
                    "(" + date + ")";
        }
    }

    /**
     * Getting currency dynamics emoji.
     *
     * @param valuteCurrent current value of Valute.
     * @param valuteBefore yesterday value of Valute.
     * @return emoji.
     */
    private String getDynamicEmojiForValuteValue(BigDecimal valuteCurrent, BigDecimal valuteBefore) {
        int compareResult = valuteCurrent.compareTo(valuteBefore);

        if (compareResult > 0) {
            return Emoji.UP_ARROW.getEmoji();
        } else if (compareResult < 0) {
            return Emoji.DOWN_ARROW.getEmoji();
        } else {
            return "";
        }
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
                .filter(valute -> valute.getCharCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * Getting current exchange rates data from service.
     *
     * @return list with data of exchange rates.
     */
    private ValCurs getValCursData() {
        return getValCursData(null);
    }

    /**
     * Getting exchange rates data from service.
     *
     * @param date exchange date.
     * @return list with data of exchange rates.
     */
    private ValCurs getValCursData(@Nullable LocalDate date) {
        String xmlUrl = "http://www.cbr.ru/scripts/XML_daily.asp";
        if (date != null) {
            xmlUrl = xmlUrl + "?date_req=" + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date);
        }

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
        ValCurs valCurs;

        try {
            valCurs = xmlMapper.readValue(networkUtils.readStringFromURL(xmlUrl, Charset.forName("windows-1251")), ValCurs.class);
        } catch (IOException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return valCurs;
    }

    @Data
    private static class ValCurs {
        @XmlAttribute
        private String name;

        @XmlAttribute(name = "Date")
        private String date;

        @XmlElement(name = "Valute")
        private List<Valute> valute;
    }

    @Data
    private static class Valute {
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

        public BigDecimal getValuteValue() {
            return new BigDecimal(this.value.replace(",", "."));
        }
    }
}
