package org.telegram.bot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.telegram.bot.utils.DateUtils.dateFormatter;
import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.TextUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Exchange implements Command {

    private static final String USD_ID = "R01235";
    private static final String EUR_ID = "R01239";
    private static final String CB_RF_DATE_FORMAT = "dd/MM/yyyy";
    private static final Integer DEFAULT_CHART_WIDTH = 1350;
    private static final Integer DEFAULT_CHART_HEIGHT = 950;
    private static final Pattern VALUTE_TO_RUB_PATTERN = Pattern.compile("^(\\d+[.,]*\\d*)\\s?([a-zA-Zа-яА-Я]+)$");
    private static final Pattern RUB_TO_VALUTE_PATTERN = Pattern.compile("^(\\d+[.,]*\\d*)\\s?([a-zA-Zа-яА-Я]+)\\.?\\s?([a-zA-Zа-яА-Я]+)$");
    private static final Pattern MONTHS_COUNT_PATTERN = Pattern.compile("^(\\d+)$");

    private final Bot bot;
    private final SpeechService speechService;
    private final CommandPropertiesService commandPropertiesService;
    private final BotStats botStats;
    private final NetworkUtils networkUtils;
    private final XmlMapper xmlMapper;
    private final Clock clock;
    private final Map<LocalDate, ValCurs> valCursDataMap = new ConcurrentHashMap<>();

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        String commandArgument = message.getCommandArgument();
        String responseText;
        Long chatId = message.getChatId();

        InputStream chart = null;
        if (commandArgument == null) {
            bot.sendTyping(chatId);
            log.debug("Request to get exchange rates for usd and eur");
            responseText = getExchangeRatesForUsdEurCny();
        } else {
            if (startsWithNumber(commandArgument)) {
                Matcher valuteToRubMatcher = VALUTE_TO_RUB_PATTERN.matcher(commandArgument);
                Matcher ruToValuteMatcher = RUB_TO_VALUTE_PATTERN.matcher(commandArgument);
                Matcher monthsCountMatcher = MONTHS_COUNT_PATTERN.matcher(commandArgument);

                if (valuteToRubMatcher.find()) {
                    bot.sendTyping(chatId);
                    String currencyCode = valuteToRubMatcher.group(2);
                    float amount =  Float.parseFloat(valuteToRubMatcher.group(1).replace(",", "."));
                    log.debug("Request to get rubles count for currency {} amount {}", currencyCode, amount);
                    responseText = getRublesForCurrencyValue(currencyCode, amount);
                } else if (ruToValuteMatcher.find()) {
                    bot.sendTyping(chatId);
                    String valuteCode = ruToValuteMatcher.group(3);
                    float rublesAmount =  Float.parseFloat(ruToValuteMatcher.group(1).replace(",", "."));
                    log.debug("Request to get valute {} count for rubles amount {}", valuteCode, rublesAmount);
                    responseText = getValuteForRublesAmount(valuteCode, rublesAmount);
                } else if (monthsCountMatcher.find()) {
                    bot.sendUploadPhoto(chatId);
                    int months = Integer.parseInt(monthsCountMatcher.group(1));

                    Pair<String, InputStream> result;
                    try {
                        result = getChartForUsdEur(months);
                    } catch (IOException e) {
                        log.error("failed to draw chart", e);
                        botStats.incrementErrors(request, e, "failed to draw chart");
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                    }

                    responseText = result.getFirst();
                    chart = result.getSecond();
                } else {
                    bot.sendTyping(chatId);
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            } else {
                bot.sendTyping(chatId);
                if (commandArgument.startsWith("_")) {
                    commandArgument = commandArgument.substring(1);
                }

                log.debug("Request to get exchange rates for {}", commandArgument);
                responseText = getExchangeRatesForCode(commandArgument.toUpperCase(Locale.ROOT));
            }
        }

        if (chart != null) {
            return returnResponse(new FileResponse(message)
                    .setText(responseText)
                    .addFile(new File(FileType.IMAGE, chart, "chart"))
                    .setResponseSettings(FormattingStyle.HTML));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    /**
     * Getting exchange rates for USD and EUR.
     *
     * @return formatted text with exchange rates.
     */
    private String getExchangeRatesForUsdEurCny() {
        LocalDate dateNow = LocalDate.now(clock);
        ValCurs valCursCurrent = getValCursData();
        String cursDate = valCursCurrent.getDate();
        ValCurs valCursBefore = getValCursData(LocalDate.parse(cursDate, dateFormatter).minusDays(1));
        ValCurs valCursTomorrow = getValCursData(dateNow.plusDays(1));
        LocalDate valCursTomorrowDate = LocalDate.parse(valCursTomorrow.getDate(), dateFormatter);

        String todayRates = buildExchangeRates(valCursCurrent.getValute(), valCursBefore.getValute(), cursDate);

        if (valCursTomorrowDate.isAfter(dateNow)) {
            todayRates = todayRates + "\n\n"
                    + buildExchangeRates(valCursTomorrow.getValute(), valCursCurrent.getValute(), valCursTomorrow.getDate());
        }

        return todayRates;
    }

    private String buildExchangeRates(List<Valute> valuteList, List<Valute> valuteListYesterday, String cursDate) {
        Valute usdCurrentValute = getValuteByCode(valuteList, "USD");
        Valute usdBeforeValute = getValuteByCode(valuteListYesterday, "USD");
        Valute eurCurrentValute = getValuteByCode(valuteList, "EUR");
        Valute eurBeforeValute = getValuteByCode(valuteListYesterday, "EUR");
        Valute cnyCurrentValute = getValuteByCode(valuteList, "CNY");
        Valute cnyBeforeValute = getValuteByCode(valuteListYesterday, "CNY");

        if (Stream.of(usdCurrentValute, usdBeforeValute, eurCurrentValute, eurBeforeValute, cnyCurrentValute, cnyBeforeValute)
                .anyMatch(Objects::isNull)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        float usdCurrent = usdCurrentValute.getValuteValue();
        float usdBefore = usdBeforeValute.getValuteValue();
        float eurCurrent = eurCurrentValute.getValuteValue();
        float eurBefore = eurBeforeValute.getValuteValue();
        float cnyCurrent = cnyCurrentValute.getValuteValue();
        float cnyBefore = cnyBeforeValute.getValuteValue();

        return "<b>${command.exchange.commoncaption}:</b>\n" +
                "$ USD = " + formatFloatValue(usdCurrent) + " RUB " + getDynamicsForValuteValue(usdCurrent, usdBefore) + "\n" +
                "€ EUR = " + formatFloatValue(eurCurrent) + " RUB " + getDynamicsForValuteValue(eurCurrent, eurBefore) + "\n" +
                "¥ CNY = " + formatFloatValue(cnyCurrent) + " RUB " + getDynamicsForValuteValue(cnyCurrent, cnyBefore) + "\n" +
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
            return "${command.exchange.valutenotfound} <b>" + code + "</b>\n${command.exchange.listofavailablevalute}: " + getValuteList(valuteList);
        } else {
            float exchangeRate = getReversExchangeRate(valute);
            return "<b>" + valute.getName() + " ${command.exchange.intorub}</b>\n" +
                    String.valueOf(amount).replace("\\.", ",") + " " + valute.getCharCode() + " = " + formatFloatValue(amount / exchangeRate) + " ₽";
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
            return "${command.exchange.valutenotfound} <b>" + valuteCode + "</b>\n${command.exchange.listofavailablevalute}: " + getValuteList(valuteList);
        } else {
            float exchangeRate = getReversExchangeRate(valute);
            return "<b>" + "${command.exchange.fromrub} " + valute.getName() + "</b>\n" +
                    String.valueOf(amount).replace("\\.", ",") + " ₽ = " + formatFloatValue(amount * exchangeRate) + " " + valute.getCharCode();
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
        ValCurs valCursBefore = getValCursData(LocalDate.parse(cursDate, dateFormatter).minusDays(1));

        List<Valute> valuteList = valCursCurrent.getValute();
        List<Valute> valuteListYesterday = valCursBefore.getValute();

        Valute valute = getValuteByCode(valuteList, code);
        if (valute == null) {
            return "${command.exchange.valutenotfound} <b>" + code + "</b>\n${command.exchange.listofavailablevalute}: " + getValuteList(valuteList);
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

    private Pair<String, InputStream> getChartForUsdEur(int months) throws IOException {
        if (months < 1 || months > 24) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        LocalDate dateNow = LocalDate.now(clock);
        LocalDate dateTo = dateNow.plusDays(2);
        LocalDate dateFrom = dateNow.minusMonths(months);

        DynamicValCurs usdValCurs = getValCursDataFromApi(dateFrom, dateTo, USD_ID);
        DynamicValCurs eurValCurs = getValCursDataFromApi(dateFrom, dateTo, EUR_ID);

        String title = "MIN/MAX\n"
                + "$ USD: <b>" + getMinValueFromRecords(usdValCurs.getRecords()) + "</b> RUB / <b>" + getMaxValueFromRecords(usdValCurs.getRecords()) + "</b> RUB\n"
                + "€ EUR: <b>" + getMinValueFromRecords(eurValCurs.getRecords()) + "</b> RUB / <b>" + getMaxValueFromRecords(eurValCurs.getRecords()) + "</b> RUB\n";

        InputStream chart = getChart(usdValCurs, eurValCurs);

        return Pair.of(title, chart);
    }

    private InputStream getChart(DynamicValCurs usdValCurs, DynamicValCurs eurValCurs) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        dataset.addSeries(getSeries(usdValCurs.getRecords(),"USD"));
        dataset.addSeries(getSeries(eurValCurs.getRecords(), "EUR"));

        List<LocalDate> datesArray = Stream.concat(
                usdValCurs.getRecords().stream().map(Record::getDate).map(date -> LocalDate.parse(date, dateFormatter)),
                eurValCurs.getRecords().stream().map(Record::getDate).map(date -> LocalDate.parse(date, dateFormatter)))
                .toList();
        LocalDate dateFrom = datesArray.stream().min(LocalDate::compareTo).orElse(LocalDate.parse(usdValCurs.getDateRange1(), dateFormatter));
        LocalDate dateTo = datesArray.stream().max(LocalDate::compareTo).orElse(LocalDate.parse(usdValCurs.getDateRange2(), dateFormatter));

        String title = "RUB " + formatDate(dateFrom) + " — " + formatDate(dateTo);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "", "", dataset);

        XYPlot plot = chart.getXYPlot();

        var renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, Color.BLUE);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        plot.getRangeAxis().setLowerBound(getMinValue(List.of(usdValCurs, eurValCurs)) - 3);
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("dd.MM.yyyy"));

        chart.getLegend().setFrame(BlockBorder.NONE);

        byte[] bytes = ChartUtils.encodeAsPNG(chart.createBufferedImage(DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT));

        return new ByteArrayInputStream(bytes);
    }

    private Double getMinValue(List<DynamicValCurs> valCurses) {
        return valCurses
                .stream()
                .map(DynamicValCurs::getRecords).
                flatMap(Collection::stream)
                .map(Record::getValuteValue)
                .min(Double::compareTo)
                .orElse(0.0);
    }

    private Double getMinValueFromRecords(List<Record> recordList) {
        return recordList
                .stream()
                .map(Record::getValuteValue)
                .min(Double::compareTo)
                .orElse(0.0);
    }

    private Double getMaxValueFromRecords(List<Record> recordList) {
        return recordList
                .stream()
                .map(Record::getValuteValue)
                .max(Double::compareTo)
                .orElse(0.0);
    }

    private XYSeries getSeries(List<Record> records, String title) {
        XYSeries series = new XYSeries(title);
        records.forEach(record -> {
            LocalDate date = LocalDate.parse(record.getDate(), dateFormatter);
            Double x = (double) date.toEpochSecond(LocalTime.MIN, OffsetDateTime.now().getOffset()) * 1000;
            Double y = record.getValuteValue() / Integer.parseInt(record.getNominal());
            series.add(x, y);
        });
        return series;
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
            emoji = Emoji.UP_ARROW.getSymbol();
        } else if (compareResult < 0) {
            emoji = Emoji.DOWN_ARROW.getSymbol();
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

        if (LocalDate.parse(valCurs.getDate(), dateFormatter).isEqual(date)) {
            valCursDataMap.put(date, valCurs);
        }

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
            xmlUrl = xmlUrl + "?date_req=" + DateTimeFormatter.ofPattern(CB_RF_DATE_FORMAT).format(date);
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

    private DynamicValCurs getValCursDataFromApi(LocalDate from, LocalDate to, String valuteId) {
        String dateFrom = DateTimeFormatter.ofPattern(CB_RF_DATE_FORMAT).format(from);
        String dateTo = DateTimeFormatter.ofPattern(CB_RF_DATE_FORMAT).format(to);

        String xmlUrl = "https://www.cbr.ru/scripts/XML_dynamic.asp?date_req1="
                + dateFrom + "&date_req2=" + dateTo + "&VAL_NM_RQ=" + valuteId;

        String response;
        try {
            response = networkUtils.readStringFromURL(xmlUrl, Charset.forName("windows-1251"));
        } catch (IOException e) {
            log.error("Error from CBRF api:", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        DynamicValCurs valCurs;
        try {
            valCurs = xmlMapper.readValue(response, DynamicValCurs.class);
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

    @Data
    public static class DynamicValCurs {
        @XmlAttribute(name = "ID")
        private String id;

        @XmlAttribute(name = "DateRange1")
        private String dateRange1;

        @XmlAttribute(name = "DateRange2")
        private String dateRange2;

        @XmlAttribute(name = "name")
        private String name;

        @XmlElement(name = "Record")
        private List<Record> records;
    }

    @Data
    public static class Record {
        @XmlAttribute(name = "Id")
        private String id;

        @XmlAttribute(name = "Date")
        private String date;

        @XmlElement(name = "Nominal")
        private String nominal;

        @XmlElement(name = "Value")
        private String value;

        public Double getValuteValue() {
            return parseDouble(this.value);
        }
    }
}
