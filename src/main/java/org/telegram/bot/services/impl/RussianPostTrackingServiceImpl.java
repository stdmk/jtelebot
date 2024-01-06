package org.telegram.bot.services.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.soap.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.TrackCodeEvent;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.PostTrackingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.config.PropertiesConfig;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;

@Service
@RequiredArgsConstructor
@Slf4j
public class RussianPostTrackingServiceImpl implements PostTrackingService {

    private static final String API_URL = "https://tracking.russianpost.ru/rtm34";

    private final PropertiesConfig propertiesConfig;
    private final BotStats botStats;
    private final SpeechService speechService;

    @Override
    public List<TrackCodeEvent> getData(String barcode) {
        log.debug("Request to update track events data of barcode {}", barcode);
        final String russianPostLogin = propertiesConfig.getRussianPostLogin();
        final String russianPostPassword = propertiesConfig.getRussianPostPassword();

        if (StringUtils.isEmpty(russianPostLogin) || StringUtils.isEmpty(russianPostPassword)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        TrackingData trackingData;
        SOAPConnection connection = getSoapConnection();
        try {
            SOAPMessage message = getSoapMessage(russianPostLogin, russianPostPassword, barcode);
            String xml = callApi(connection, message);
            trackingData = parseTrackingData(xml);
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

    private String callApi(SOAPConnection connection, SOAPMessage message) {
        try {
            SOAPMessage soapResponse = connection.call(message, API_URL);

            botStats.incrementRussianPostRequests();

            Source sourceContent = soapResponse.getSOAPPart().getContent();

            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "html");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();

            StreamResult result = new StreamResult(writer);
            t.transform(sourceContent, result);

            return writer.toString();
        } catch (SOAPException | TransformerException e) {
            log.error("Failed to call api: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

    private TrackingData parseTrackingData(String xml) {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
        try {
            return xmlMapper.readValue(xml, TrackingData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse TrackingData: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
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
                .collect(Collectors.toList());
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
    private static class TrackingData {
        @XmlElement(name = "Body")
        private Body body;
    }

    @Data
    private static class Body {
        @XmlElement(name = "getOperationHistoryResponse")
        private OperationHistoryResponse operationHistoryResponse;

        /**
         * Ошибки.
         */
        @XmlElement(name = "Fault")
        private Fault fault;
    }

    @Data
    private static class Fault {
        /**
         * Код ошибки.
         */
        @XmlElement(name = "Code")
        private Code code;

        /**
         * Причина ошибки
         */
        @XmlElement(name = "Reason")
        private Reason reason;

        /**
         * Описание ошибки
         */
        @XmlElement(name = "Detail")
        private Detail detail;
    }

    @Data
    private static class Code {
        @XmlElement(name = "Value")
        private String value;
    }

    @Data
    private static class Reason {
        @XmlElement(name = "Text")
        private String text;
    }

    @Data
    private static class Detail {
        @XmlElement(name = "OperationHistoryFaultReason")
        private String operationHistoryFaultReason;

        @XmlElement(name = "AuthorizationFaultReason")
        private String authorizationFaultReason;

        @XmlElement(name = "LanguageFaultReason")
        private String languageFaultReason;
    }

    @Data
    private static class OperationHistoryResponse {
        @XmlElement(name = "OperationHistoryData")
        private OperationHistoryData operationHistoryData;
    }

    @Data
    private static class OperationHistoryData {
        @XmlElement(name = "historyRecord")
        private List<HistoryRecord> historyRecord;
    }

    @Data
    private static class HistoryRecord {
        /**
         * Содержит адресные данные с операцией над отправлением.
         */
        @XmlElement(name = "AddressParameters")
        private AddressParameters addressParameters;

        /**
         * Содержит финансовые данные, связанные с операцией над почтовым отправлением.
         */
        @XmlElement(name = "FinanceParameters")
        private FinanceParameters financeParameters;

        /**
         * Содержит данные о почтовом отправлении.
         */
        @XmlElement(name = "ItemParameters")
        private ItemParameters itemParameters;

        /**
         * Содержит параметры операции над отправлением.
         */
        @XmlElement(name = "OperationParameters")
        private OperationParameters operationParameters;

        /**
         * Содержит данные субъектов, связанных с операцией над почтовым отправлением.
         */
        @XmlElement(name = "UserParameters")
        private UserParameters userParameters;
    }

    @Data
    private static class AddressParameters {
        /**
         * Содержит адресные данные места назначения пересылки отправления.
         */
        @XmlElement(name = "DestinationAddress")
        private Address destinationAddress;

        /**
         * Содержит адресные данные места проведения операции над отправлением.
         */
        @XmlElement(name = "OperationAddress")
        private Address operationAddress;

        /**
         * Содержит данные о стране места назначения пересылки отправления.
         */
        @XmlElement(name = "MailDirect")
        private AddressParameter mailDirect;

        /**
         * Содержит данные о стране приема почтового отправления.
         */
        @XmlElement(name = "CountryFrom")
        private AddressParameter countryFrom;

        /**
         * Содержит данные о стране проведения операции над почтовым отправлением.
         */
        @XmlElement(name = "CountryOper")
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
        @XmlElement(name = "Index")
        private String index;

        /**
         * Адрес и/или название места назначения. Пример значения.
         */
        @XmlElement(name = "Description")
        private String description;
    }

    @Data
    private static class AddressParameter {
        @XmlElement(name = "Id")
        private String id;

        @XmlElement(name = "Code2A")
        private String code2A;

        @XmlElement(name = "Code3A")
        private String code3a;

        @XmlElement(name = "NameRU")
        private String nameRu;

        @XmlElement(name = "NameEN")
        private String nameEn;
    }

    @Data
    private static class FinanceParameters {
        /**
         * Сумма наложенного платежа в копейках.
         */
        @XmlElement(name = "Payment")
        private Long payment;

        /**
         * Сумма объявленной ценности в копейках.
         */
        @XmlElement(name = "Value")
        private Long value;

        /**
         * Общая сумма платы за пересылку наземным и воздушным транспортом в копейках.
         */
        @XmlElement(name = "MassRate")
        private Long massRate;

        /**
         * Сумма платы за объявленную ценность в копейках.
         */
        @XmlElement(name = "InsrRate")
        private Long insrRate;

        /**
         * Выделенная сумма платы за пересылку воздушным транспортом из общей суммы платы за пересылку в копейках.
         */
        @XmlElement(name = "AirRate")
        private Long airRate;

        /**
         * Сумма дополнительного тарифного сбора в копейках.
         */
        @XmlElement(name = "Rate")
        private Long rate;

        /**
         * Сумма таможенного платежа в копейках.
         */
        @XmlElement(name = "CustomDuty")
        private Long customDuty;
    }

    @Data
    private static class ItemParameters {
        /**
         * Идентификатор почтового отправления, текущий для данной операции.
         */
        @XmlElement(name = "Barcode")
        private String barcode;

        /**
         * Служебная информация, идентифицирующая отправление, может иметь значение ДМ квитанции, связанной с отправлением или иметь значение <null>
         */
        @XmlElement(name = "Internum")
        private String internum;

        /**
         * Признак корректности вида и категории отправления для внутренней пересылки
         */
        @XmlElement(name = "ValidRuType")
        private Boolean validRuType;

        /**
         * Признак корректности вида и категории отправления для международной пересылки
         */
        @XmlElement(name = "ValidEnType")
        private Boolean validEnType;

        /**
         * Содержит текстовое описание вида и категории отправления.
         */
        @XmlElement(name = "ComplexItemName")
        private String complexItemName;

        //TOOD распарсить отметки в енаме
        /**
         * Содержит информацию о разряде почтового отправления.
         */
        @XmlElement(name = "MailRank")
        private DictionaryElement mailRank;

        /**
         * Содержит информацию об отметках почтовых отправлений.
         */
        @XmlElement(name = "PostMark")
        private DictionaryElement postMark;

        /**
         * Содержит данные о виде почтового отправления.
         */
        @XmlElement(name = "MailType")
        private DictionaryElement mailType;

        /**
         * Содержит данные о категории почтового отправления.
         */
        @XmlElement(name = "MailCtg")
        private DictionaryElement mailCtg;

        /**
         * Вес отправления в граммах.
         */
        @XmlElement(name = "Mass")
        private Long mass;

        /**
         * Значение максимально возможного веса для данного вида и категории отправления для внутренней пересылки.
         */
        @XmlElement(name = "MaxMassRu")
        private Long maxMassRus;

        /**
         * Значение максимально возможного веса для данного вида и категории отправления для международной пересылки.
         */
        @XmlElement(name = "MaxMassEn")
        private Long maxMassEn;
    }

    @Data
    private static class OperationParameters {
        /**
         * Содержит информацию об операции над отправлением.
         */
        @XmlElement(name = "OperType")
        private DictionaryElement operType;

        /**
         * Содержит информацию об атрибуте операции над отправлением.
         */
        @XmlElement(name = "OperAttr")
        private DictionaryElement operAttr;

        /**
         * Содержит данные о дате и времени проведения операции над отправлением.
         * <p>
         * Пример значения: 2015-01-08T14:50:00.000+03:00
         */
        @XmlElement(name = "OperDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm a z")
        private Date operDate;
    }

    @Data
    public static class UserParameters {
        /**
         * Содержит информацию о категории отправителя.
         */
        @XmlElement(name = "SendCtg")
        private DictionaryElement sendCtg;

        /**
         * Содержит данные об отправителе.
         * <p>
         * Пример значения: ИВАНОВ А Н
         */
        @XmlElement(name = "Sndr")
        private String sndr;

        /**
         * Содержит данные о получателе отправления.
         * <p>
         * Пример значения: ПЕТРОВ И.К.
         */
        @XmlElement(name = "Rcpn")
        private String rcpn;
    }

    @Data
    private static class DictionaryElement {
        /**
         * Код отметки почтового отправления.
         */
        @XmlElement(name = "Id")
        private String id;

        /**
         * Наименование отметки почтового отправления.
         */
        @XmlElement(name = "Name")
        private String name;
    }
}