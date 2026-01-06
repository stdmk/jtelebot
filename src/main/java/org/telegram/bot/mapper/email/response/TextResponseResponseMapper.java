package org.telegram.bot.mapper.email.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.Keyboard;
import org.telegram.bot.domain.model.response.TextResponse;

@RequiredArgsConstructor
@Component
public class TextResponseResponseMapper implements EmailResponseMapper {

    private final KeyboardEmailMapper keyboardEmailMapper;

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return TextResponse.class;
    }

    @Override
    public EmailResponse map(BotResponse botResponse) {
        TextResponse textResponse = (TextResponse) botResponse;

        String keyboardString;
        Keyboard keyboard = textResponse.getKeyboard();
        if (keyboard != null) {
            keyboardString = "<br><br>" + keyboardEmailMapper.keyboardToString(keyboard);
        } else {
            keyboardString = "";
        }

        return new EmailResponse().setText(textResponse.getText() + keyboardString);
    }

}
