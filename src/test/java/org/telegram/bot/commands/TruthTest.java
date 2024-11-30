package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TruthTest {

    @Mock
    private Bot bot;
    @Mock
    private RestTemplate botRestTemplate;

    @InjectMocks
    private Truth truth;

    @Test
    void parseWithRestClientExceptionTest() {
        BotRequest request = TestUtils.getRequestFromGroup("/truth");

        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException("error"));

        BotResponse botResponse = truth.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(request.getMessage().getMessageId(), textResponse.getReplyToMessageId());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithReplyToMessageTest() {
        BotRequest request = TestUtils.getRequestWithRepliedMessage("tratatam-tratatam");
        String image = "image";

        ResponseEntity response = mock(ResponseEntity.class);
        when(response.getBody()).thenReturn(new Truth.YesNo().setImage(image));
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        BotResponse botResponse = truth.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);
        assertEquals(fileResponse.getFiles().get(0).getUrl(), image);
        assertEquals(request.getMessage().getReplyToMessage().getMessageId(), fileResponse.getReplyToMessageId());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

}