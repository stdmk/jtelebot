package org.telegram.bot.services.impl;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.xml.soap.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.TrackCodeEvent;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.PostTrackingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RussianPostTrackingServiceImpl implements PostTrackingService {

    private static final String API_URL = "https://tracking.russianpost.ru/rtm34";

    private final PropertiesConfig propertiesConfig;
    private final BotStats botStats;
    private final SpeechService speechService;

    private final XmlMapper xmlMapper;

    @Override
    public List<TrackCodeEvent> getData(String barcode) {
        log.debug("Request to update track events data of barcode {}", barcode);
        final String russianPostLogin = propertiesConfig.getRussianPostLogin();
        final String russianPostPassword = propertiesConfig.getRussianPostPassword();

        if (TextUtils.isEmpty(russianPostLogin) || TextUtils.isEmpty(russianPostPassword)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        TrackingData trackingData;
        SOAPConnection connection = getSoapConnection();
        try {
            SOAPMessage message = getSoapMessage(russianPostLogin, russianPostPassword, barcode);
            SOAPMessage soapResponse = callApi(connection, message);
            trackingData = parseTrackingData(soapResponse);
            checkForErrors(trackingData);
        } catch (BotException botException) {
            log.error("Failed to update TrackCodeEvents by {}", barcode);
            throw botException;
        } catch (Exception e) {
            try {
                connection.close();
            } catch (SOAPException soapException) {
                log.warn("Failed to close SOAPConnection: ", soapException);
            }
            throw e;
        }

        return mapTrackingDataToTrackCodeEventList(trackingData);
    }

    private SOAPConnection getSoapConnection() {
        try {
            return SOAPConnectionFactory.newInstance().createConnection();
        } catch (SOAPException e) {
            log.error("Failed to create SOAPConnection: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    private SOAPMessage getSoapMessage(String russianPostLogin, String russianPostPassword, String barcode) {
        try {
            MessageFactory messageFactory = MessageFactory.newInstance("SOAP 1.2 Protocol");
            SOAPMessage message = messageFactory.createMessage();

            SOAPPart soapPart = message.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            SOAPBody body = envelope.getBody();

            envelope.addNamespaceDeclaration("soap","http://www.w3.org/2003/05/soap-envelope");
            envelope.addNamespaceDeclaration("oper","http://russianpost.org/operationhistory");
            envelope.addNamespaceDeclaration("data","http://russianpost.org/operationhistory/data");
            envelope.addNamespaceDeclaration("soapenv","http://schemas.xmlsoap.org/soap/envelope/");

            SOAPElement operElement = body.addChildElement("getOperationHistory", "oper");
            SOAPElement dataElement = operElement.addChildElement("OperationHistoryRequest","data");
            SOAPElement soapBarcode = dataElement.addChildElement("Barcode","data");
            SOAPElement messageType = dataElement.addChildElement("MessageType","data");
            SOAPElement language = dataElement.addChildElement("Language","data");
            SOAPElement dataAuth = operElement.addChildElement("AuthorizationHeader","data");

            SOAPFactory sf = SOAPFactory.newInstance();
            Name must = sf.createName("mustUnderstand","soapenv","http://schemas.xmlsoap.org/soap/envelope/");
            dataAuth.addAttribute(must,"1");

            SOAPElement login = dataAuth.addChildElement("login", "data");
            SOAPElement password = dataAuth.addChildElement("password","data");

            soapBarcode.addTextNode(barcode);
            messageType.addTextNode("0");
            language.addTextNode("RUS");
            login.addTextNode(russianPostLogin);
            password.addTextNode(russianPostPassword);

            message.saveChanges();

            return message;
        } catch (SOAPException e) {
            log.error("Failed to create SOAPMessage: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    private SOAPMessage callApi(SOAPConnection connection, SOAPMessage message) {
        try {
            SOAPMessage soapResponse = connection.call(message, API_URL);

            botStats.incrementRussianPostRequests();

            return soapResponse;
        } catch (SOAPException e) {
            log.error("Failed to call api: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    private TrackingData parseTrackingData(SOAPMessage soapResponse) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            soapResponse.writeTo(out);
            String xml = out.toString(StandardCharsets.UTF_8);
            Envelope envelope = xmlMapper.readValue(xml, Envelope.class);
            TrackingData trackingData = new TrackingData();
            trackingData.setBody(envelope.getBody());
            return trackingData;
        } catch (SOAPException | java.io.IOException e) {
            log.error("Failed to parse TrackingData", e);
            throw new BotException(
                    speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)
            );
        }
    }


    private List<TrackCodeEvent> mapTrackingDataToTrackCodeEventList(TrackingData trackingData) {
        List<HistoryRecord> historyRecords = Optional.ofNullable(trackingData.getBody())
                .map(Body::getOperationHistoryResponse)
                .map(OperationHistoryResponse::getOperationHistoryData)
                .map(OperationHistoryData::getHistoryRecord)
                .orElse(new ArrayList<>());

        return historyRecords
                .stream()
                .map(historyRecord -> {
                    TrackCodeEvent trackCodeEvent = new TrackCodeEvent();

                    Optional.ofNullable(historyRecord.getItemParameters())
                            .ifPresent(itemParameters -> {
                                Optional.ofNullable(itemParameters.getBarcode())
                                        .ifPresent(trackCodeEvent::setEventBarcode);

                                Optional.ofNullable(itemParameters.getComplexItemName())
                                        .ifPresent(trackCodeEvent::setItemName);

                                Optional.ofNullable(itemParameters.getMass())
                                        .ifPresent(trackCodeEvent::setGram);
                            });

                    Optional.ofNullable(historyRecord.getOperationParameters())
                            .ifPresent(operationParameters -> {
                                Optional.ofNullable(operationParameters.getOperDate())
                                        .ifPresent(date ->
                                                trackCodeEvent.setEventDateTime(
                                                        LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())));

                                Optional.ofNullable(operationParameters.getOperType())
                                        .map(DictionaryElement::getName)
                                        .ifPresent(trackCodeEvent::setOperationType);

                                Optional.ofNullable(operationParameters.getOperAttr())
                                        .map(DictionaryElement::getName)
                                        .ifPresent(trackCodeEvent::setOperationDescription);
                            });

                    Optional.ofNullable(historyRecord.getAddressParameters())
                            .ifPresent(addressParameters -> {
                                Optional.ofNullable(addressParameters.getOperationAddress())
                                        .ifPresent(address -> trackCodeEvent.setAddress(address.getDescription()).setIndex(address.getIndex()));
                                Optional.ofNullable(addressParameters.getCountryFrom())
                                        .ifPresent(address -> trackCodeEvent.setCountryFrom(address.getNameRu()));
                                Optional.ofNullable(addressParameters.getMailDirect())
                                        .ifPresent(address -> trackCodeEvent.setCountryTo(address.getNameRu()));
                            });

                    Optional.ofNullable(historyRecord.getUserParameters())
                            .ifPresent(userParameters -> {
                                Optional.ofNullable(userParameters.getSndr())
                                        .ifPresent(trackCodeEvent::setSender);
                                Optional.ofNullable(userParameters.getRcpn())
                                        .ifPresent(trackCodeEvent::setRecipient);
                            });

                    return trackCodeEvent;
                })
                .toList();
    }

    private void checkForErrors(TrackingData trackingData) {
        Optional.ofNullable(trackingData.getBody())
                .map(Body::getFault)
                .map(Fault::getDetail)
                .ifPresent(detail -> {
                    String errorText = detail.getOperationHistoryFaultReason();
                    if (errorText == null) {
                        errorText = detail.getAuthorizationFaultReason();
                        if (errorText == null) {
                            errorText = detail.getLanguageFaultReason();
                        }
                    }

                    log.debug("Received error from Russian Post api: {}", errorText);
                    throw new BotException("Ответ от Почты России: " + errorText);
                });
    }

    @Data
    private static class Envelope {
        @JacksonXmlProperty(localName = "Body")
        private Body body;
    }

    @Data
    private static class TrackingData {
        @JacksonXmlProperty(localName = "Body")
        private Body body;
    }

    @Data
    private static class Body {
        @JacksonXmlProperty(localName = "getOperationHistoryResponse")
        private OperationHistoryResponse operationHistoryResponse;

        /**
         * Ошибки.
         */
        @JacksonXmlProperty(localName = "Fault")
        private Fault fault;
    }

    @Data
    private static class Fault {
        /**
         * Код ошибки.
         */
        @JacksonXmlProperty(localName = "Code")
        private Code code;

        /**
         * Причина ошибки
         */
        @JacksonXmlProperty(localName = "Reason")
        private Reason reason;

        /**
         * Описание ошибки
         */
        @JacksonXmlProperty(localName = "Detail")
        private Detail detail;
    }

    @Data
    private static class Code {
        @JacksonXmlProperty(localName = "Value")
        private String value;
    }

    @Data
    private static class Reason {
        @JacksonXmlProperty(localName = "Text")
        private String text;
    }

    @Data
    private static class Detail {
        @JacksonXmlProperty(localName = "OperationHistoryFaultReason")
        private String operationHistoryFaultReason;

        @JacksonXmlProperty(localName = "AuthorizationFaultReason")
        private String authorizationFaultReason;

        @JacksonXmlProperty(localName = "LanguageFaultReason")
        private String languageFaultReason;
    }

    @Data
    private static class OperationHistoryResponse {
        @JacksonXmlProperty(localName = "OperationHistoryData")
        private OperationHistoryData operationHistoryData;
    }

    @Data
    private static class OperationHistoryData {
        @JacksonXmlProperty(localName = "historyRecord")
        private List<HistoryRecord> historyRecord;
    }

    @Data
    private static class HistoryRecord {
        /**
         * Содержит адресные данные с операцией над отправлением.
         */
        @JacksonXmlProperty(localName = "AddressParameters")
        private AddressParameters addressParameters;

        /**
         * Содержит финансовые данные, связанные с операцией над почтовым отправлением.
         */
        @JacksonXmlProperty(localName = "FinanceParameters")
        private FinanceParameters financeParameters;

        /**
         * Содержит данные о почтовом отправлении.
         */
        @JacksonXmlProperty(localName = "ItemParameters")
        private ItemParameters itemParameters;

        /**
         * Содержит параметры операции над отправлением.
         */
        @JacksonXmlProperty(localName = "OperationParameters")
        private OperationParameters operationParameters;

        /**
         * Содержит данные субъектов, связанных с операцией над почтовым отправлением.
         */
        @JacksonXmlProperty(localName = "UserParameters")
        private UserParameters userParameters;
    }

    @Data
    private static class AddressParameters {
        /**
         * Содержит адресные данные места назначения пересылки отправления.
         */
        @JacksonXmlProperty(localName = "DestinationAddress")
        private Address destinationAddress;

        /**
         * Содержит адресные данные места проведения операции над отправлением.
         */
        @JacksonXmlProperty(localName = "OperationAddress")
        private Address operationAddress;

        /**
         * Содержит данные о стране места назначения пересылки отправления.
         */
        @JacksonXmlProperty(localName = "MailDirect")
        private AddressParameter mailDirect;

        /**
         * Содержит данные о стране приема почтового отправления.
         */
        @JacksonXmlProperty(localName = "CountryFrom")
        private AddressParameter countryFrom;

        /**
         * Содержит данные о стране проведения операции над почтовым отправлением.
         */
        @JacksonXmlProperty(localName = "CountryOper")
        private AddressParameter countryOper;
    }

    /**
     * Содержит адресные данные места назначения пересылки отправления.
     */
    @Data
    private static class Address {

        /**
         * Почтовый индекс места назначения. Не возвращается для зарубежных операций.
         */
        @JacksonXmlProperty(localName = "Index")
        private String index;

        /**
         * Адрес и/или название места назначения. Пример значения.
         */
        @JacksonXmlProperty(localName = "Description")
        private String description;
    }

    @Data
    private static class AddressParameter {
        @JacksonXmlProperty(localName = "Id")
        private String id;

        @JacksonXmlProperty(localName = "Code2A")
        private String code2A;

        @JacksonXmlProperty(localName = "Code3A")
        private String code3a;

        @JacksonXmlProperty(localName = "NameRU")
        private String nameRu;

        @JacksonXmlProperty(localName = "NameEN")
        private String nameEn;
    }

    @Data
    private static class FinanceParameters {
        /**
         * Сумма наложенного платежа в копейках.
         */
        @JacksonXmlProperty(localName = "Payment")
        private Long payment;

        /**
         * Сумма объявленной ценности в копейках.
         */
        @JacksonXmlProperty(localName = "Value")
        private Long value;

        /**
         * Общая сумма платы за пересылку наземным и воздушным транспортом в копейках.
         */
        @JacksonXmlProperty(localName = "MassRate")
        private Long massRate;

        /**
         * Сумма платы за объявленную ценность в копейках.
         */
        @JacksonXmlProperty(localName = "InsrRate")
        private Long insrRate;

        /**
         * Выделенная сумма платы за пересылку воздушным транспортом из общей суммы платы за пересылку в копейках.
         */
        @JacksonXmlProperty(localName = "AirRate")
        private Long airRate;

        /**
         * Сумма дополнительного тарифного сбора в копейках.
         */
        @JacksonXmlProperty(localName = "Rate")
        private Long rate;

        /**
         * Сумма таможенного платежа в копейках.
         */
        @JacksonXmlProperty(localName = "CustomDuty")
        private Long customDuty;
    }

    @Data
    private static class ItemParameters {
        /**
         * Идентификатор почтового отправления, текущий для данной операции.
         */
        @JacksonXmlProperty(localName = "Barcode")
        private String barcode;

        /**
         * Служебная информация, идентифицирующая отправление, может иметь значение ДМ квитанции, связанной с отправлением или иметь значение <null>
         */
        @JacksonXmlProperty(localName = "Internum")
        private String internum;

        /**
         * Признак корректности вида и категории отправления для внутренней пересылки
         */
        @JacksonXmlProperty(localName = "ValidRuType")
        private Boolean validRuType;

        /**
         * Признак корректности вида и категории отправления для международной пересылки
         */
        @JacksonXmlProperty(localName = "ValidEnType")
        private Boolean validEnType;

        /**
         * Содержит текстовое описание вида и категории отправления.
         */
        @JacksonXmlProperty(localName = "ComplexItemName")
        private String complexItemName;

        //TOOD распарсить отметки в енаме
        /**
         * Содержит информацию о разряде почтового отправления.
         */
        @JacksonXmlProperty(localName = "MailRank")
        private DictionaryElement mailRank;

        /**
         * Содержит информацию об отметках почтовых отправлений.
         */
        @JacksonXmlProperty(localName = "PostMark")
        private DictionaryElement postMark;

        /**
         * Содержит данные о виде почтового отправления.
         */
        @JacksonXmlProperty(localName = "MailType")
        private DictionaryElement mailType;

        /**
         * Содержит данные о категории почтового отправления.
         */
        @JacksonXmlProperty(localName = "MailCtg")
        private DictionaryElement mailCtg;

        /**
         * Вес отправления в граммах.
         */
        @JacksonXmlProperty(localName = "Mass")
        private Long mass;

        /**
         * Значение максимально возможного веса для данного вида и категории отправления для внутренней пересылки.
         */
        @JacksonXmlProperty(localName = "MaxMassRu")
        private Long maxMassRus;

        /**
         * Значение максимально возможного веса для данного вида и категории отправления для международной пересылки.
         */
        @JacksonXmlProperty(localName = "MaxMassEn")
        private Long maxMassEn;
    }

    @Data
    private static class OperationParameters {
        /**
         * Содержит информацию об операции над отправлением.
         */
        @JacksonXmlProperty(localName = "OperType")
        private DictionaryElement operType;

        /**
         * Содержит информацию об атрибуте операции над отправлением.
         */
        @JacksonXmlProperty(localName = "OperAttr")
        private DictionaryElement operAttr;

        /**
         * Содержит данные о дате и времени проведения операции над отправлением.
         * <p>
         * Пример значения: 2015-01-08T14:50:00.000+03:00
         */
        @JacksonXmlProperty(localName = "OperDate")
                private Date operDate;
    }

    @Data
    public static class UserParameters {
        /**
         * Содержит информацию о категории отправителя.
         */
        @JacksonXmlProperty(localName = "SendCtg")
        private DictionaryElement sendCtg;

        /**
         * Содержит данные об отправителе.
         * <p>
         * Пример значения: ИВАНОВ А Н
         */
        @JacksonXmlProperty(localName = "Sndr")
        private String sndr;

        /**
         * Содержит данные о получателе отправления.
         * <p>
         * Пример значения: ПЕТРОВ И.К.
         */
        @JacksonXmlProperty(localName = "Rcpn")
        private String rcpn;
    }

    @Data
    private static class DictionaryElement {
        /**
         * Код отметки почтового отправления.
         */
        @JacksonXmlProperty(localName = "Id")
        private String id;

        /**
         * Наименование отметки почтового отправления.
         */
        @JacksonXmlProperty(localName = "Name")
        private String name;
    }
}