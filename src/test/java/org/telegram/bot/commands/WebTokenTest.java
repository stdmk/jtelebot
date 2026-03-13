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
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.WebTokenService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebTokenTest {

    @Mock
    private WebTokenService webTokenService;

    @InjectMocks
    private WebToken webToken;

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestFromPrivate("webtoken");
        when(webTokenService.createOrUpdateToken(TestUtils.DEFAULT_USER_ID)).thenReturn("test-token");

        BotResponse botResponse = webToken.parse(request).getFirst();
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse, FormattingStyle.HTML);

        assertEquals("Ваш токен для входа в web-интерфейс:\n<pre><code>test-token</code></pre>", textResponse.getText());
    }
}
