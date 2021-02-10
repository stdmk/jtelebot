package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.XML;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

import static org.telegram.bot.utils.NetworkUtils.readStringFromURL;

@Component
@AllArgsConstructor
public class Exchange implements CommandParent<SendMessage> {

    private final SpeechService speechService;
    private final CommandPropertiesService commandPropertiesService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        if (textMessage == null) {
            responseText = prepareUsdAndEurResponseText();
        } else {
            if (textMessage.startsWith("_")) {
                textMessage = textMessage.substring(1);
            }
            responseText = prepareResponseTextForCode(textMessage.toUpperCase(Locale.ROOT));
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String prepareUsdAndEurResponseText() throws BotException {
        List<Valute> valuteList = getValCursData();

        Valute usdValute = getValute(valuteList, "USD");
        Valute eurValute = getValute(valuteList, "EUR");

        return "<b>Курс валют ЦБ РФ:</b>\n" +
                "$ USD = " + usdValute.getValue() + " RUB\n" +
                "€ EUR = " + eurValute.getValue() + " RUB";
    }

    private String prepareResponseTextForCode(String code) throws BotException {
        List<Valute> valuteList = getValCursData();

        Valute valute = getValute(valuteList, code);
        if (valute == null) {
            return "Не нашёл валюту <b>" + code + "</b>\nСписок доступных: " + prepareResponseTextWithValuteList(valuteList);
        } else {
            return "<b>" + valute.getName() + "</b>\n" +
                    valute.getNominal() + " " + valute.getCharCode() + " = " + valute.getValue() + " RUB\n" +
                   "1 RUB = " + Float.parseFloat(valute.getNominal().replaceAll(",", ".")) / Float.parseFloat(valute.getValue().replaceAll(",", ".")) + " " + valute.getCharCode();
        }
    }

    private String prepareResponseTextWithValuteList(List<Valute> valuteList) {
        StringBuilder buf = new StringBuilder();
        String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();

        valuteList
                .forEach(valute -> buf.append(valute.getName()).append(" - /").append(commandName).append("_").append(valute.getCharCode().toLowerCase(Locale.ROOT)).append("\n"));

        return buf.toString();
    }

    private Valute getValute(List<Valute> valuteList, String code) {
        return valuteList
                .stream()
                .filter(valute -> valute.getCharCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    private List<Valute> getValCursData() throws BotException {
        final String xmlUrl = "http://www.cbr.ru/scripts/XML_daily.asp";
        ObjectMapper mapper = new ObjectMapper();
        ValData valData;

        try {
            valData = mapper.readValue(XML.toJSONObject(readStringFromURL(xmlUrl, Charset.forName("windows-1251"))).toString(), ValData.class);
        } catch (IOException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return valData.getValCurs().getValute();
    }

    @Data
    private static class ValData {
        @JsonProperty("ValCurs")
        private ValCurs valCurs;
    }

    @Data
    private static class ValCurs {
        private String name;

        @JsonProperty("Date")
        private String date;

        @JsonProperty("Valute")
        private List<Valute> valute;
    }

    @Data
    private static class Valute {
        @JsonProperty("CharCode")
        private String charCode;

        @JsonProperty("Value")
        private String value;

        @JsonProperty("ID")
        private String id;

        @JsonProperty("Nominal")
        private String nominal;

        @JsonProperty("NumCode")
        private String numCode;

        @JsonProperty("Name")
        private String name;
    }
}
