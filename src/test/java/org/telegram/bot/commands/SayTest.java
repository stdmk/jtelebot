package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.services.CommandWaitingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SayTest {

    @Mock
    private CommandWaitingService commandWaitingService;

    @InjectMocks
    private Say say;

    @Test
    void parseWithoutArgumentTest() {
        final String expectedResponseText = "${command.say.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("say");

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());

        BotResponse botResponse = say.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseTest() {
        final String expectedResponseText = "test";
        BotRequest request = TestUtils.getRequestFromGroup("say " + expectedResponseText);

        when(commandWaitingService.getText(request.getMessage())).thenReturn(request.getMessage().getCommandArgument());

        BotResponse botResponse = say.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponseText, textResponse.getText());
    }

}