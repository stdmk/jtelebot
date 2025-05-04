package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@RequiredArgsConstructor
@Component
public class SendMessageMapperText implements TelegramTextApiMethodMapper {

    private final KeyboardMapper keyboardMapper;
    private final ParseModeMapper parseModeMapper;

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return TextResponse.class;
    }

    public SendMessage map(BotResponse botResponse) {
        TextResponse textResponse = (TextResponse) botResponse;

        SendMessage sendMessage = new SendMessage(textResponse.getChatId().toString(), textResponse.getText());
        sendMessage.setReplyToMessageId(textResponse.getReplyToMessageId());
        sendMessage.setReplyMarkup(keyboardMapper.toKeyboard(textResponse.getKeyboard()));

        ResponseSettings responseSettings = textResponse.getResponseSettings();
        if (responseSettings != null) {
            if (Boolean.FALSE.equals(responseSettings.getNotification())) {
                sendMessage.disableNotification();
            }
            if (Boolean.FALSE.equals(responseSettings.getWebPagePreview())) {
                sendMessage.disableWebPagePreview();
            }

            sendMessage.setParseMode(parseModeMapper.toParseMode(responseSettings.getFormattingStyle()));
        }

        return sendMessage;
    }

}
