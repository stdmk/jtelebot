package org.telegram.bot.mapper.email.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.Keyboard;
import org.telegram.bot.domain.model.response.TextResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextResponseResponseMapperTest {

    @Mock
    private KeyboardEmailMapper keyboardEmailMapper;

    @InjectMocks
    private TextResponseResponseMapper textResponseResponseMapper;

    @Test
    void getMappingClassTest() {
        Class<TextResponse> expected = TextResponse.class;
        Class<? extends BotResponse> actual = textResponseResponseMapper.getMappingClass();
        assertEquals(expected, actual);
    }

    @Test
    void mapTest() {
        final String text = "text";
        final String keyboardText = "keyboardText";
        final String expectedEmailText = text + "<br><br>" + keyboardText;
        Keyboard keyboard = new Keyboard();
        TextResponse textResponse = new TextResponse(new Message().setChat(new Chat()))
                .setText(text)
                .setKeyboard(keyboard);
        when(keyboardEmailMapper.keyboardToString(keyboard)).thenReturn(keyboardText);

        EmailResponse emailResponse = textResponseResponseMapper.map(textResponse);

        assertEquals(expectedEmailText, emailResponse.getText());
    }

}