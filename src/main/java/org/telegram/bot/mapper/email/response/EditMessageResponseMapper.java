package org.telegram.bot.mapper.email.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EditResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.Keyboard;

@RequiredArgsConstructor
@Component
public class EditMessageResponseMapper implements EmailResponseMapper {

    private final KeyboardEmailMapper keyboardEmailMapper;

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return EditResponse.class;
    }

    @Override
    public EmailResponse map(BotResponse botResponse) {
        EditResponse editResponse = (EditResponse) botResponse;

        String keyboardString;
        Keyboard keyboard = editResponse.getKeyboard();
        if (keyboard != null) {
            keyboardString = "<br><br>" + keyboardEmailMapper.keyboardToString(keyboard);
        } else {
            keyboardString = "";
        }

        return new EmailResponse()
                .setText("${mapper.email.edit.caption}: " + editResponse.getEditableMessageId()
                        + "<br>" + editResponse.getText()
                        + keyboardString);
    }
}
