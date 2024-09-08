package org.telegram.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqlTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private Sql sql;

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(sql, "entityManager", entityManager);
    }

    @Test
    void parseWithoutArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("sql");
        List<BotResponse> botResponses = sql.parse(request);

        assertTrue(botResponses.isEmpty());
        verify(bot, never()).sendTyping(anyLong());
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void parseWithUnknownTypeOfQueryTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("sql tratatam");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> sql.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(anyLong());
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void parseWithDbExceptionTest() {
        final String errorText = "error";
        final String expectedResponse = "${command.sql.error}: `error`";
        BotRequest request = TestUtils.getRequestFromGroup("sql select 1");

        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException(errorText));

        BotResponse botResponse = sql.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());
    }

    @Test
    void parseWithEmptyResponseForSelectQueryTest() {
        final String expectedResponse = "```${command.sql.emptyresponse}```";
        BotRequest request = TestUtils.getRequestFromGroup("sql select 1");

        Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(List.of());
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        BotResponse botResponse = sql.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());
    }

    @Test
    void parseWithSelectQueryTest() {
        final String expectedResponse = "```[1, 2, 3]```";
        BotRequest request = TestUtils.getRequestFromGroup("sql select 1");

        Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(List.of("1", "2", "3"));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        BotResponse botResponse = sql.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());
    }

    @Test
    void parseWithSelectQueryObjectsTest() {
        final String expectedResponse = """
                ```[1, 2, 3]
                [4, 5, 6]```""";
        BotRequest request = TestUtils.getRequestFromGroup("sql select 1");

        Query query = mock(Query.class);
        when(query.getResultList()).thenReturn(List.of(new Object[]{"1", "2", "3"}, new Object[]{"4", "5", "6"}));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        BotResponse botResponse = sql.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete", "insert"})
    void parseWithUpdateQueryObjectsTest(String command) {
        final String expectedResponse = "${command.sql.success}: `0`";
        BotRequest request = TestUtils.getRequestFromGroup("sql " + command + " 1");

        Query query = mock(Query.class);
        when(query.executeUpdate()).thenReturn(0);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        BotResponse botResponse = sql.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());
    }

}