package org.telegram.bot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class Xml implements Command {

    @Qualifier("xmlMapper")
    private final ObjectMapper xmlMapper;

    private final SpeechService speechService;

    private final Bot bot;

    private final BotStats botStats;

    private Transformer transformer;

    @PostConstruct
    private void postConstruct() throws TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 2);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        String commandArgument = message.getCommandArgument();

        String responseText;
        String fileName = null;
        if (commandArgument == null) {
            if (!message.hasAttachment() || !MessageContentType.FILE.equals(message.getMessageContentType())) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            Attachment attachment = message.getAttachments().get(0);
            fileName = attachment.getName();

            InputStream file;
            try {
                file = bot.getInputStreamFromTelegramFile(attachment.getFileId());
            } catch (TelegramApiException | IOException e) {
                log.error("Failed to get file from telegram", e);
                botStats.incrementErrors(request, e, "Failed to get file from telegram");
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            try {
                responseText = format(new String(file.readAllBytes(), StandardCharsets.UTF_8));
            } catch (JsonProcessingException | TransformerException e) {
                fileName = null;
                responseText = "`" + e.getMessage() + "`";
            } catch (Exception e) {
                log.error("Failed to read bytes of file from telegram", e);
                botStats.incrementErrors(request, e, "Failed to read bytes of file from telegram");
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        } else {
            try {
                responseText = "```xml\n" + format(commandArgument) + "```";
            } catch (Exception e) {
                responseText = "`" + e.getMessage() + "`";
            }
        }

        if (fileName != null) {
            return returnResponse(new FileResponse(message)
                    .addFile(new File(
                            FileType.FILE, new ByteArrayInputStream(responseText.getBytes(StandardCharsets.UTF_8)), fileName)));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private String format(String xml) throws JsonProcessingException, TransformerException {
        String result;

        validateXml(xml);
        if (TextUtils.hasLineBreaks(xml)) {
            result = linearize(xml);
        } else {
            result = beauty(xml);
        }

        return result;
    }

    private void validateXml(String xml) throws JsonProcessingException {
        xmlMapper.readTree(xml);
    }

    private String beauty(String xml) throws TransformerException {
        Source xmlInput = new StreamSource(new StringReader(xml));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);

        transformer.transform(xmlInput, xmlOutput);
        return xmlOutput.getWriter().toString();
    }

    @SneakyThrows
    private String linearize(String xml) {
        BufferedReader br = new BufferedReader(new StringReader(xml));
        String line;
        StringBuilder sb = new StringBuilder();

        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }

        return sb.toString();
    }


}
