package org.telegram.bot.mapper.email.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EditResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.Keyboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditMessageResponseMapperTest {

    @Mock
    private KeyboardEmailMapper keyboardEmailMapper;

    @InjectMocks
    private EditMessageResponseMapper editMessageResponseMapper;

    @Test
    void getMappingClassTest() {
        Class<EditResponse> expected = EditResponse.class;
        Class<? extends BotResponse> actual = editMessageResponseMapper.getMappingClass();
        assertEquals(expected, actual);
    }

    @Test
    void mapTest() {
        final int messageId = 1;
        final String newText = "text";
        final String keyboardText = "keyboardText";
        final String expectedEmailText = "${mapper.email.edit.caption}: 1<br>" + newText + "<br><br>" + keyboardText;
        Keyboard keyboard = new Keyboard();
        EditResponse editResponse = new EditResponse(new Message().setChat(new Chat()).setMessageId(messageId))
                .setText(newText)
                .setKeyboard(keyboard);

        when(keyboardEmailMapper.keyboardToString(keyboard)).thenReturn(keyboardText);

        EmailResponse emailResponse = editMessageResponseMapper.map(editResponse);

        assertEquals(expectedEmailText, emailResponse.getText());
    }

}