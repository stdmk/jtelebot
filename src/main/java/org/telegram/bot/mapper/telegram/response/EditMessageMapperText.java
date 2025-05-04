package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EditResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

@RequiredArgsConstructor
@Component
public class EditMessageMapperText implements TelegramTextApiMethodMapper {

    private final KeyboardMapper keyboardMapper;
    private final ParseModeMapper parseModeMapper;

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return EditResponse.class;
    }

    @Override
    public EditMessageText map(BotResponse botResponse) {
        EditResponse editResponse = (EditResponse) botResponse;

        EditMessageText editMessageText = new EditMessageText(editResponse.getText());

        editMessageText.setChatId(editResponse.getChatId());
        editMessageText.setMessageId(editResponse.getEditableMessageId());
        editMessageText.setReplyMarkup(keyboardMapper.toKeyboard(editResponse.getKeyboard()));

        ResponseSettings responseSettings = editResponse.getResponseSettings();
        if (responseSettings != null) {
            if (Boolean.FALSE.equals(responseSettings.getWebPagePreview())) {
                editMessageText.disableWebPagePreview();
            }

            editMessageText.setParseMode(parseModeMapper.toParseMode(responseSettings.getFormattingStyle()));
        }

        return editMessageText;
    }

}
