package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.services.BotStats;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResponseMapperTest {

    private final TelegramTextApiMethodMapper textMapper = mock(TelegramTextApiMethodMapper.class);
    private final TelegramFileApiMethodMapper fileMapper = mock(TelegramFileApiMethodMapper.class);

    private final MediaGroupMapper mediaGroupMapper = mock(MediaGroupMapper.class);
    private final BotStats botStats = mock(BotStats.class);

    private final ResponseMapper responseMapper = new ResponseMapper(List.of(textMapper), List.of(fileMapper), mediaGroupMapper, botStats);

    @BeforeEach
    void init() {
        doReturn(TextResponse.class).when(textMapper).getMappingClass();
        when(fileMapper.getMappingFileType()).thenReturn(FileType.FILE);

        ReflectionTestUtils.invokeMethod(responseMapper, "postConstruct");
    }

    @Test
    void toTelegramTextMethodUnknownResponseTest() {
        BotResponse response = mock(BotResponse.class);
        final String expectedErrorMessage = "Unknown type of BotResponse: " + response.getClass();
        List<BotResponse> responses = List.of(response);

        IllegalArgumentException illegalArgumentException = assertThrows((IllegalArgumentException.class), () -> responseMapper.toTelegramMethod(responses));

        assertEquals(expectedErrorMessage, illegalArgumentException.getMessage());

        verify(botStats).incrementErrors(response, expectedErrorMessage);
    }

    @Test
    void toTelegramTextMethodResponseTest() {
        BotResponse response = new TextResponse();
        List<BotResponse> responses = List.of(response);

        PartialBotApiMethod partialBotApiMethod = mock(PartialBotApiMethod.class);
        when(textMapper.map(response)).thenReturn(partialBotApiMethod);

        List<PartialBotApiMethod<?>> partialBotApiMethods = responseMapper.toTelegramMethod(responses);

        assertEquals(partialBotApiMethods.get(0), partialBotApiMethod);
    }

    @Test
    void toTelegramFileMethodUnknownResponseTest() {
        FileResponse response = new FileResponse().addFile(new File(FileType.IMAGE, "url"));
        List<BotResponse> responses = List.of(response);

        PartialBotApiMethod partialBotApiMethod = mock(PartialBotApiMethod.class);
        when(fileMapper.map(response)).thenReturn(partialBotApiMethod);

        List<PartialBotApiMethod<?>> partialBotApiMethods = assertDoesNotThrow(() -> responseMapper.toTelegramMethod(responses));

        assertEquals(partialBotApiMethods.get(0), partialBotApiMethod);

        verify(botStats).incrementErrors(response, "Unable to find fileMapper for FileType: " + FileType.IMAGE);
    }

    @Test
    void toTelegramFileMethodTest() {
        FileResponse response = new FileResponse().addFile(new File(FileType.FILE, "url"));
        List<BotResponse> responses = List.of(response);

        PartialBotApiMethod partialBotApiMethod = mock(PartialBotApiMethod.class);
        when(fileMapper.map(response)).thenReturn(partialBotApiMethod);

        List<PartialBotApiMethod<?>> partialBotApiMethods = responseMapper.toTelegramMethod(responses);

        assertEquals(partialBotApiMethods.get(0), partialBotApiMethod);
    }

    @Test
    void toTelegramFileMethodWhenSeveralFilesTest() {
        FileResponse response = new FileResponse().addFile(new File(FileType.IMAGE, "url")).addFile(new File(FileType.IMAGE, "url2"));
        List<BotResponse> responses = List.of(response);

        PartialBotApiMethod partialBotApiMethod = mock(PartialBotApiMethod.class);
        when(mediaGroupMapper.map(response)).thenReturn(partialBotApiMethod);

        List<PartialBotApiMethod<?>> partialBotApiMethods = responseMapper.toTelegramMethod(responses);

        assertEquals(partialBotApiMethods.get(0), partialBotApiMethod);
    }

}