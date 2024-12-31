package org.telegram.bot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.DelayCommand;
import org.telegram.bot.domain.entities.DisableCommand;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelayTest {

    private static final String CURRENT_DATE = DateUtils.formatDate(LocalDate.now());
    private static final String CURRENT_DATE_WITHOUT_YEAR = DateTimeFormatter.ofPattern("dd.MM").format(LocalDate.now());

    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private CommandPropertiesService commandPropertiesService;
    @Mock
    private DisableCommandService disableCommandService;
    @Mock
    private DelayCommandService delayCommandService;
    @Mock
    private UserService userService;
    @Mock
    private UserCityService userCityService;
    @Mock
    private Bot bot;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private Delay delay;

    @BeforeEach
    void init() {
        when(internationalizationService.getAllTranslations("command.delay.second")).thenReturn(Set.of("с", "s"));
        when(internationalizationService.getAllTranslations("command.delay.minutes")).thenReturn(Set.of("м", "m"));
        when(internationalizationService.getAllTranslations("command.delay.hours")).thenReturn(Set.of("ч", "h"));

        ReflectionTestUtils.invokeMethod(delay, "postConstruct");
    }

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = "${command.delay.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("delay");

        BotResponse botResponse = delay.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());
        verify(commandWaitingService).add(request.getMessage(), Delay.class);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @ParameterizedTest
    @MethodSource("provideDateTimes")
    void parseWithDateTimeAsArgumentTest(String dateTimeString) throws JsonProcessingException {
        final String command = "echo";
        final String expectedResponseText = "saved";
        final LocalDateTime expectedDateTime = LocalDate.now().atTime(0, 1, 0);
        final String expectedRequestJson = "";
        BotRequest request = TestUtils.getRequestFromGroup("delay " + dateTimeString + " " + command);
        Message message = request.getMessage();

        when(bot.getBotUsername()).thenReturn("jtelebot");
        CommandProperties commandProperties = new CommandProperties().setAccessLevel(1);
        when(commandPropertiesService.findCommandInText(command, "jtelebot")).thenReturn(commandProperties);
        when(userService.isUserHaveAccessForCommand(message.getUser(), commandProperties.getAccessLevel())).thenReturn(true);
        when(objectMapper.writeValueAsString(request)).thenReturn(expectedRequestJson);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = delay.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<DelayCommand> delayCommandArgumentCaptor = ArgumentCaptor.forClass(DelayCommand.class);
        verify(delayCommandService).save(delayCommandArgumentCaptor.capture());

        DelayCommand delayCommand = delayCommandArgumentCaptor.getValue();
        assertEquals(expectedDateTime, delayCommand.getDateTime());
        assertEquals(expectedRequestJson, delayCommand.getRequestJson());

        verify(bot).sendTyping(message.getChatId());
    }

    private static Stream<String> provideDateTimes() {
        return Stream.of(
                CURRENT_DATE + " 00:01:00",
                CURRENT_DATE + " 00:01",
                CURRENT_DATE_WITHOUT_YEAR + " 00:01",
                CURRENT_DATE_WITHOUT_YEAR + " 00:01",
                "00:01:00",
                "00:01"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT3600S", "PT60M", "PT1H"})
    void parseWithDurationAsArgumentTest(String durationString) throws JsonProcessingException {
        final String command = "echo";
        final String expectedResponseText = "saved";
        final LocalDateTime expectedDateTime = LocalDateTime.now().plusHours(1);
        final String expectedRequestJson = "";
        BotRequest request = TestUtils.getRequestFromGroup("delay " + durationString + " " + command);
        Message message = request.getMessage();

        when(bot.getBotUsername()).thenReturn("jtelebot");
        CommandProperties commandProperties = new CommandProperties().setAccessLevel(1);
        when(commandPropertiesService.findCommandInText(command, "jtelebot")).thenReturn(commandProperties);
        when(userService.isUserHaveAccessForCommand(message.getUser(), commandProperties.getAccessLevel())).thenReturn(true);
        when(objectMapper.writeValueAsString(request)).thenReturn(expectedRequestJson);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = delay.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<DelayCommand> delayCommandArgumentCaptor = ArgumentCaptor.forClass(DelayCommand.class);
        verify(delayCommandService).save(delayCommandArgumentCaptor.capture());

        DelayCommand delayCommand = delayCommandArgumentCaptor.getValue();
        assertEquals(expectedDateTime.withNano(0), delayCommand.getDateTime().withNano(0));
        assertEquals(expectedRequestJson, delayCommand.getRequestJson());

        verify(bot).sendTyping(message.getChatId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3600s", "60m", "1h"})
    void parseWithSimpleDurationAsArgumentTest(String simpleDuratuionString) throws JsonProcessingException {
        final String command = "echo";
        final String expectedResponseText = "saved";
        final LocalDateTime expectedDateTime = LocalDateTime.now().plusHours(1);
        final String expectedRequestJson = "";
        BotRequest request = TestUtils.getRequestFromGroup("delay " + simpleDuratuionString + " " + command);

        when(bot.getBotUsername()).thenReturn("jtelebot");
        CommandProperties commandProperties = new CommandProperties().setAccessLevel(1);
        when(commandPropertiesService.findCommandInText(command, "jtelebot")).thenReturn(commandProperties);
        when(userService.isUserHaveAccessForCommand(request.getMessage().getUser(), commandProperties.getAccessLevel())).thenReturn(true);
        when(objectMapper.writeValueAsString(request)).thenReturn(expectedRequestJson);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse botResponse = delay.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<DelayCommand> delayCommandArgumentCaptor = ArgumentCaptor.forClass(DelayCommand.class);
        verify(delayCommandService).save(delayCommandArgumentCaptor.capture());

        DelayCommand delayCommand = delayCommandArgumentCaptor.getValue();
        assertEquals(expectedDateTime.withNano(0), delayCommand.getDateTime().withNano(0));
        assertEquals(expectedRequestJson, delayCommand.getRequestJson());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWrongInputTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("delay 15 hours echo");
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());

        BotException botException = assertThrows(BotException.class, () -> delay.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseDurationMoreThenOneYearAsArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("delay 9000h echo");
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());

        BotException botException = assertThrows(BotException.class, () -> delay.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithUnknownCommandAsArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("delay 15s figecho");
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());

        BotException botException = assertThrows(BotException.class, () -> delay.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithDisabledCommandAsArgumentTest() {
        final String command = "echo";
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("delay 15s " + command);
        Message message = request.getMessage();

        when(bot.getBotUsername()).thenReturn("jtelebot");
        CommandProperties commandProperties = new CommandProperties().setAccessLevel(1);
        when(commandPropertiesService.findCommandInText(command, "jtelebot")).thenReturn(commandProperties);
        when(disableCommandService.get(message.getChat(), commandProperties)).thenReturn(new DisableCommand());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());

        BotException botException = assertThrows(BotException.class, () -> delay.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithNoAccessToCommandTest() {
        final String command = "echo";
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("delay 15s " + command);
        Message message = request.getMessage();

        when(bot.getBotUsername()).thenReturn("jtelebot");
        CommandProperties commandProperties = new CommandProperties().setAccessLevel(1);
        when(commandPropertiesService.findCommandInText(command, "jtelebot")).thenReturn(commandProperties);
        when(userService.isUserHaveAccessForCommand(message.getUser(), commandProperties.getAccessLevel())).thenReturn(false);
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS)).thenReturn(expectedErrorText);
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());

        BotException botException = assertThrows(BotException.class, () -> delay.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithSerializationErrorTest() throws JsonProcessingException {
        final String command = "echo";
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("delay 15s " + command);
        Message message = request.getMessage();

        when(bot.getBotUsername()).thenReturn("jtelebot");
        CommandProperties commandProperties = new CommandProperties().setAccessLevel(1);
        when(commandPropertiesService.findCommandInText(command, "jtelebot")).thenReturn(commandProperties);
        when(userService.isUserHaveAccessForCommand(message.getUser(), commandProperties.getAccessLevel())).thenReturn(true);
        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(request)).thenThrow(jsonProcessingException);
        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);
        when(userCityService.getZoneIdOfUserOrDefault(message)).thenReturn(ZoneId.systemDefault());

        BotException botException = assertThrows(BotException.class, () -> delay.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

}