package org.telegram.bot.commands;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Qr implements Command, MessageAnalyzer {

    private static final Map<DecodeHintType, Boolean> HINTS = Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE);

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final BotStats botStats;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String commandArgument = commandWaitingService.getText(message);

        if (commandArgument == null) {
            bot.sendTyping(message.getChatId());
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            return returnResponse(new TextResponse(message)
                    .setText("${command.qr.commandwaitingstart}"));
        } else {
            bot.sendUploadPhoto(message.getChatId());
            return returnResponse(new FileResponse(message)
                    .setText(commandArgument)
                    .addFile(new File(FileType.IMAGE, generateQrFromText(commandArgument), "qr")));
        }
    }

    private InputStream generateQrFromText(String text) {
        Map<EncodeHintType, Object> map = new EnumMap<>(EncodeHintType.class);
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
    public List<BotResponse> analyze(BotRequest request) {
        Message message = request.getMessage();

        if (message.hasAttachment() && MessageContentType.PHOTO.equals(message.getMessageContentType())) {
            return message.getAttachments()
                    .stream()
                    .max(Comparator.comparing(Attachment::getSize))
                    .map(photo -> {
                        BufferedImage image;

                        try (InputStream inputStream = bot.getInputStreamFromTelegramFile(photo.getFileId())) {
                            image = ImageIO.read(inputStream);
                        } catch (IOException e) {
                            log.error("Failed to read image", e);
                            botStats.incrementErrors(request, e, "Failed to read image");
                            return null;
                        } catch (TelegramApiException e) {
                            log.error("Failed to get file from telegram", e);
                            botStats.incrementErrors(request, e, "Failed to get file from telegram");
                            return null;
                        }

                        String decodeResult;
                        try {
                            decodeResult = getDecodeResult(image);
                        } catch (NotFoundException e) {
                            log.debug("QR is missing");
                            return null;
                        }

                        return decodeResult;
                    })
                    .map(decodeResult -> returnResponse(new TextResponse(message)
                            .setText(decodeResult)
                            .setResponseSettings(FormattingStyle.HTML)))
                    .orElse(returnResponse());
        }

        return returnResponse();
    }

    private String getDecodeResult(BufferedImage image) throws NotFoundException {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));

        Reader reader = new MultiFormatReader();
        MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
        Result[] results = multiReader.decodeMultiple(bitmap, HINTS);
        if (results != null) {
            StringBuilder buf = new StringBuilder();
            for (Result result : results) {
                BarcodeFormat barcodeFormat = result.getBarcodeFormat();
                if (barcodeFormat == null) {
                    barcodeFormat = BarcodeFormat.QR_CODE;
                }

                buf.append(barcodeFormat.name()).append(": <code>").append(result.getText()).append("</code>\n");
            }

            return buf.toString();
        }

        return null;
    }

}
