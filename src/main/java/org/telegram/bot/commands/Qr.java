package org.telegram.bot.commands;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Qr implements Command<PartialBotApiMethod<?>>, TextAnalyzer {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final BotStats botStats;
    private final NetworkUtils networkUtils;

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);

        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        Long chatId = message.getChatId();
        if (textMessage == null) {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());

            SendMessage sendMessage = new SendMessage();
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setChatId(chatId);
            sendMessage.setText("${command.qr.commandwaitingstart}");

            return sendMessage;
        } else {
            bot.sendUploadPhoto(chatId);
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(generateQrFromText(textMessage), "qr"));
            sendPhoto.setCaption(textMessage);
            sendPhoto.setReplyToMessageId(message.getMessageId());
            sendPhoto.setChatId(chatId);

            return sendPhoto;
        }
    }

    private InputStream generateQrFromText(String text) {
        Map<EncodeHintType, Object> map = new HashMap<>();
        map.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8);

        BitMatrix matrix;
        try {
            matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 500, 500, map);
        } catch (WriterException e) {
            log.error("Encoding text to qr error: {}", e.getMessage());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "png", os);
        } catch (IOException e) {
            log.error("Creating qr-image error: {}", e.getMessage());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return new ByteArrayInputStream(os.toByteArray());
    }

    @Override
    public void analyze(Update update) {
        Message message = getMessageFromUpdate(update);

        if (message.hasPhoto()) {
            List<PhotoSize> photoList = message.getPhoto();
            BufferedImage image;

            try {
                image = ImageIO.read(networkUtils.getInputStreamFromTelegramFile(photoList.get(photoList.size() - 1).getFileId()));
            } catch (TelegramApiException | IOException e) {
                log.error("Failed to get file from telegram: {}", e.getMessage());
                return;
            }

            String textFromQr;
            try {
                textFromQr = getTextFromQr(image);
            } catch (NotFoundException e) {
                log.debug("QR is missing");
                return;
            }

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setText("QR: " + textFromQr);

            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Cannot send response: {}", e.getMessage());
                botStats.incrementErrors(update, sendMessage, e, "Error sending QR code decryption");
            }
        }
    }

    private String getTextFromQr(BufferedImage image) throws NotFoundException {
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(binaryBitmap);

        return result.getText();
    }
}
