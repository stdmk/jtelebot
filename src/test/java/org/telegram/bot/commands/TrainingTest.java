package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTest {

    private static final LocalDateTime CURRENT_DATE_TIME = LocalDateTime.of(2000, 1, 1, 23, 0, 0);

    private static final Long TRAINING1_ID = 1L;
    private static final String TRAINING1_NAME = "name1";
    private static final LocalTime TRAINING1_TIME_START = LocalTime.of(7, 0);
    private static final LocalTime TRAINING1_TIME_END = LocalTime.of(8, 0);
    private static final Float TRAINING1_COST = 1F;
    private static final Long TRAINING2_ID = 2L;
    private static final String TRAINING2_NAME = "name2";
    private static final LocalTime TRAINING2_TIME_START = LocalTime.of(18, 0);
    private static final LocalTime TRAINING2_TIME_END = LocalTime.of(19, 0);
    private static final Float TRAINING2_COST = 1F;
    private static final Long TRAINING3_ID = 3L;
    private static final String TRAINING3_NAME = "name3";
    private static final LocalTime TRAINING3_TIME_START = LocalTime.of(19, 30);
    private static final LocalTime TRAINING3_TIME_END = LocalTime.of(20, 0);
    private static final Float TRAINING3_COST = 0.5F;

    private static final Long ACTIVE_SUBSCRIPTION_ID = 1L;
    private static final Integer ACTIVE_SUBSCRIPTION_COUNT = 50;
    private static final Float ACTIVE_SUBSCRIPTION_COUNT_LEFT = 25.5F;
    private static final LocalDate ACTIVE_SUBSCRIPTION_DATE_START = CURRENT_DATE_TIME.minusDays(7).toLocalDate();
    private static final Period ACTIVE_SUBSCRIPTION_PERIOD = Period.of(0, 3, 0);

    private static final Long INACTIVE_SUBSCRIPTION_ID = 2L;
    private static final Integer INACTIVE_SUBSCRIPTION_COUNT = 50;
    private static final Float INACTIVE_SUBSCRIPTION_COUNT_LEFT = 0F;
    private static final LocalDate INACTIVE_SUBSCRIPTION_DATE_START = CURRENT_DATE_TIME.minusMonths(1).toLocalDate();
    private static final Period INACTIVE_SUBSCRIPTION_PERIOD = Period.of(0, 3, 0);

    private static final String EXPECTED_REPORT = """
                ${command.training.begginingwith} <b>01.01.2000</b>
                ${command.training.trainings}:  <b>30</b>
                ${command.training.canceledtrainings}: <b>15</b> (50%)
                ${command.training.unplannedtrainings}: <b>15</b> (50%)
                -----------------------------
                name1: <b>10</b> (33%)
                name2: <b>10</b> (33%)
                name3: <b>10</b> (33%)
                -----------------------------
                ${command.training.totaltime}: <b>1 ${utils.date.d}. 1 ${utils.date.h}. </b>
                ${command.training.totalcost}: <b>25.0</b>
                """;
    private static final String EXPECTED_REPORT_FILE_CONTENT = """
            ${command.training.begginingwith} 01.01.2000
            ${command.training.trainings}:  30
            ${command.training.canceledtrainings}: 15 (50%)
            ${command.training.unplannedtrainings}: 15 (50%)
            -----------------------------
            name1: 10 (33%)
            name2: 10 (33%)
            name3: 10 (33%)
            -----------------------------
            ${command.training.totaltime}: 1 ${utils.date.d}. 1 ${utils.date.h}.\s
            ${command.training.totalcost}: 25.0
            -----------------------------

            01.01.2000 Sat.
            name1 (1.0)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name1 (1.0)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name1 (1.0)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name1 (1.0)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name1 (1.0)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            01.01.2000 Sat.
            name2 (1.0)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name2 (1.0)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name2 (1.0)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name2 (1.0)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name2 (1.0)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            01.01.2000 Sat.
            name3 (0.5)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name3 (0.5)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name3 (0.5)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name3 (0.5)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name3 (0.5)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            01.01.2000 Sat.
            name1 (1.0)
            ${command.training.canceled} (reason0)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name1 (1.0)
            ${command.training.canceled} (reason1)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name1 (1.0)
            ${command.training.canceled} (reason2)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name1 (1.0)
            ${command.training.canceled} (reason3)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name1 (1.0)
            ${command.training.canceled} (reason4)
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            01.01.2000 Sat.
            name2 (1.0)
            ${command.training.canceled} (reason0)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name2 (1.0)
            ${command.training.canceled} (reason1)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name2 (1.0)
            ${command.training.canceled} (reason2)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name2 (1.0)
            ${command.training.canceled} (reason3)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name2 (1.0)
            ${command.training.canceled} (reason4)
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            01.01.2000 Sat.
            name3 (0.5)
            ${command.training.canceled} (reason0)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name3 (0.5)
            ${command.training.canceled} (reason1)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name3 (0.5)
            ${command.training.canceled} (reason2)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name3 (0.5)
            ${command.training.canceled} (reason3)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name3 (0.5)
            ${command.training.canceled} (reason4)
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            01.01.2000 Sat.
            name1 (1.0)
            ${command.training.unplanned}
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name1 (1.0)
            ${command.training.unplanned}
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name1 (1.0)
            ${command.training.unplanned}
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name1 (1.0)
            ${command.training.unplanned}
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name1 (1.0)
            ${command.training.unplanned}
            07:00 — 08:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            01.01.2000 Sat.
            name2 (1.0)
            ${command.training.unplanned}
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name2 (1.0)
            ${command.training.unplanned}
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name2 (1.0)
            ${command.training.unplanned}
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name2 (1.0)
            ${command.training.unplanned}
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name2 (1.0)
            ${command.training.unplanned}
            18:00 — 19:00 (1 ${utils.date.h}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            01.01.2000 Sat.
            name3 (0.5)
            ${command.training.unplanned}
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            02.01.2000 Sun.
            name3 (0.5)
            ${command.training.unplanned}
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            03.01.2000 Mon.
            name3 (0.5)
            ${command.training.unplanned}
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            04.01.2000 Tue.
            name3 (0.5)
            ${command.training.unplanned}
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            05.01.2000 Wed.
            name3 (0.5)
            ${command.training.unplanned}
            19:30 — 20:00 (30 ${utils.date.m}. )
            ${command.training.subscription}: 25.12.1999 — 25.03.2000 (50)

            """;

    @Mock
    private Bot bot;
    @Mock
    private TrainingScheduledService trainingScheduledService;
    @Mock
    private TrainSubscriptionService trainSubscriptionService;
    @Mock
    private TrainingEventService trainingEventService;
    @Mock
    private TrainingService trainingService;
    @Mock
    private TrainingStoppedService trainingStoppedService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private Clock clock;

    @InjectMocks
    private Training training;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void parseWithoutArgumentTest(boolean callback) {
        final String expectedResponseText = """
                <b>${command.training.trainingstoday}:</b>

                <b>${command.training.trainingstomorrow}:</b>
                -----------------------------
                <b>${command.training.currentsubscription}:</b>
                ${command.training.notpresent}""";
        BotRequest request;
        if (callback) {
            request = TestUtils.getRequestWithCallback("training");
        } else {
            request = TestUtils.getRequestFromPrivate("training");
        }

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = training.parse(request).get(0);

        Keyboard keyboard;
        if (callback) {
            EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
            assertEquals(expectedResponseText, editResponse.getText());
            keyboard = editResponse.getKeyboard();
        } else {
            TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
            assertEquals(expectedResponseText, textResponse.getText());
            keyboard = textResponse.getKeyboard();
        }

        assertMainKeyboard(keyboard);

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithoutArgumentTrainingsStoppedTest() {
        final String expectedResponseText = """
                <u>${command.training.schedulestopped}</u>

                <b>${command.training.trainingstoday}:</b>

                <b>${command.training.trainingstomorrow}:</b>
                -----------------------------
                <b>${command.training.currentsubscription}:</b>
                ${command.training.notpresent}""";
        BotRequest request = TestUtils.getRequestWithCallback("training");
        Message message = request.getMessage();

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(trainingStoppedService.isStopped(message.getUser())).thenReturn(true);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertMainKeyboard(editResponse.getKeyboard());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithoutArgumentAllDataTest() {
        final String expectedResponseText = """
                <b>${command.training.trainingstoday}:</b>
                07:00 — name1 (1.0)
                18:00 — name2 (1.0)

                <b>${command.training.trainingstomorrow}:</b>
                -----------------------------
                <b>${command.training.currentsubscription}:</b> (50)
                ${command.training.sellby}: <b>25.03.2000</b>
                ${command.training.trainingsleft}: <b>25.5</b>
                ${command.training.enoughuntil}: <b>24.06.2000</b>""";
        BotRequest request = TestUtils.getRequestWithCallback("training");
        Message message = request.getMessage();

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        User user = message.getUser();
        List<org.telegram.bot.domain.entities.Training> trainings = getSomeTrainings();
        TrainSubscription subscription = getSomeActiveTrainingSubscription();
        when(trainingScheduledService.get(user))
                .thenReturn(List.of(
                        new TrainingScheduled().setId(1L).setUser(user).setTraining(trainings.get(0)).setDayOfWeek(DayOfWeek.SATURDAY)));
        when(trainingEventService.getAllUnplanned(user, CURRENT_DATE_TIME.toLocalDate()))
                .thenReturn(List.of(
                        new TrainingEvent().setId(2L).setUser(user).setDateTime(CURRENT_DATE_TIME.minusHours(2)).setTraining(trainings.get(1)).setTrainSubscription(subscription).setCanceled(false).setUnplanned(true)));
        when(trainingEventService.getAllCanceled(user, CURRENT_DATE_TIME.toLocalDate()))
                .thenReturn(List.of(
                        new TrainingEvent().setId(3L).setUser(user).setDateTime(CURRENT_DATE_TIME.minusHours(3)).setTraining(trainings.get(2)).setTrainSubscription(subscription).setCanceled(true).setCancellationReason("reason1")));
        when(trainSubscriptionService.getFirstActive(user)).thenReturn(subscription);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertMainKeyboard(editResponse.getKeyboard());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseUnknownCommandCallbackTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("training_x");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseAddTrainingEmptyCommandButNomenclatureNotSetTest() {
        final String expectedResponseText = "<b>${command.training.choosetraining}:</b>${command.training.emptynomenclature} /set";
        BotRequest request = TestUtils.getRequestWithCallback("training_add");
        Message message = request.getMessage();

        when(trainingService.get(message.getUser())).thenReturn(Collections.emptyList());

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertMainKeyboard(editResponse.getKeyboard());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseAddTrainingEmptyCommandTest() {
        final String expectedResponseText = "<b>${command.training.choosetraining}:</b>";
        BotRequest request = TestUtils.getRequestWithCallback("training_add");
        Message message = request.getMessage();

        when(trainingService.get(message.getUser())).thenReturn(getSomeTrainings());
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(4, keyboardButtonsList.size());

        List<KeyboardButton> addRow1 = keyboardButtonsList.get(0);
        assertEquals(1, addRow1.size());
        KeyboardButton addButton1 = addRow1.get(0);
        assertEquals("\uD83C\uDD95" + TRAINING1_NAME + " " + TRAINING1_TIME_START + " (" + TRAINING1_COST + ")", addButton1.getName());
        assertEquals("training_add" + TRAINING1_ID, addButton1.getCallback());

        List<KeyboardButton> addRow2 = keyboardButtonsList.get(1);
        assertEquals(1, addRow2.size());
        KeyboardButton addButton2 = addRow2.get(0);
        assertEquals("\uD83C\uDD95" + TRAINING2_NAME + " " + TRAINING2_TIME_START + " (" + TRAINING2_COST + ")", addButton2.getName());
        assertEquals("training_add" + TRAINING2_ID, addButton2.getCallback());

        List<KeyboardButton> addRow3 = keyboardButtonsList.get(2);
        assertEquals(1, addRow3.size());
        KeyboardButton addButton3 = addRow3.get(0);
        assertEquals("\uD83C\uDD95" + TRAINING3_NAME + " " + TRAINING3_TIME_START + " (" + TRAINING3_COST + ")", addButton3.getName());
        assertEquals("training_add" + TRAINING3_ID, addButton3.getCallback());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseAddNotPastTrainingTest() {
        final String expectedErrorText = "<b>${command.training.choosetraining}:</b>";
        BotRequest request = TestUtils.getRequestWithCallback("training_add" + TRAINING1_ID);
        Message message = request.getMessage();

        org.telegram.bot.domain.entities.Training notPastTraining = getSomeTrainings().get(0);
        notPastTraining.setTimeStart(CURRENT_DATE_TIME.plusMinutes(1).toLocalTime());
        when(trainingService.get(message.getUser(), TRAINING1_ID)).thenReturn(notPastTraining);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseAddTrainingTest() {
        final String expectedSavedText = "saved";
        final String expectedResponseText = expectedSavedText + "\n" +
                "-----------------------------\n" +
                "<b>${command.training.trainingstoday}:</b>\n" +
                "\n" +
                "<b>${command.training.trainingstomorrow}:</b>\n" +
                "-----------------------------\n" +
                "<b>${command.training.currentsubscription}:</b> (50)\n" +
                "${command.training.sellby}: <b>25.03.2000</b>\n" +
                "${command.training.trainingsleft}: <b>25.5</b>\n";
        BotRequest request = TestUtils.getRequestWithCallback("training_add" + TRAINING1_ID);
        Message message = request.getMessage();

        org.telegram.bot.domain.entities.Training training1 = getSomeTrainings().get(0);
        when(trainingService.get(message.getUser(), TRAINING1_ID)).thenReturn(training1);
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        TrainSubscription subscription = getSomeActiveTrainingSubscription();
        subscription.setCountLeft(ACTIVE_SUBSCRIPTION_COUNT_LEFT + 1);
        when(trainSubscriptionService.getFirstActive(message.getUser())).thenReturn(subscription);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedSavedText);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertMainKeyboard(editResponse.getKeyboard());

        assertEquals(ACTIVE_SUBSCRIPTION_COUNT_LEFT, subscription.getCountLeft());
        verify(trainSubscriptionService).save(subscription);

        ArgumentCaptor<TrainingEvent> trainingEventCaptor = ArgumentCaptor.forClass(TrainingEvent.class);
        verify(trainingEventService).save(trainingEventCaptor.capture());
        TrainingEvent trainingEvent = trainingEventCaptor.getValue();
        assertEquals(message.getUser(), trainingEvent.getUser());
        assertEquals(training1, trainingEvent.getTraining());
        assertEquals(subscription, trainingEvent.getTrainSubscription());
        assertEquals(CURRENT_DATE_TIME, trainingEvent.getDateTime());
        assertFalse(trainingEvent.getCanceled());
        assertTrue(trainingEvent.getUnplanned());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseEmptyCancelCommandTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("training_c");
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(commandWaitingService).remove(message.getChat(), message.getUser());
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCancelCommandWithoutReasonTest() {
        final String expectedResponseText = "${command.training.rejectionreason}";
        final long eventId = 1L;
        final String expectedCommandWaitingText = "training_c" + eventId + "_cr";
        BotRequest request = TestUtils.getRequestFromPrivate("training_c" + eventId);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        BotResponse botResponse = training.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        assertNull(textResponse.getKeyboard());

        verify(commandWaitingService).add(chat, user, Training.class, expectedCommandWaitingText);
        verify(commandWaitingService).remove(chat, user);
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCancelReasonForNotExistenceEventTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromPrivate("test");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        CommandWaiting commandWaiting = new CommandWaiting().setTextMessage("training_c1_cr ");
        when(commandWaitingService.get(chat, user)).thenReturn(commandWaiting);
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(commandWaitingService).remove(chat, user);
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseCancelCommandTest() {
        final String expectedResponseText = "saved";
        final Long eventId = 1L;
        final String cancelReason = "test";
        BotRequest request = TestUtils.getRequestFromPrivate(cancelReason);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        CommandWaiting commandWaiting = new CommandWaiting().setTextMessage("training_c1_cr ");
        when(commandWaitingService.get(chat, user)).thenReturn(commandWaiting);
        TrainSubscription subscription = getSomeActiveTrainingSubscription();
        TrainingEvent event = new TrainingEvent()
                .setId(eventId)
                .setUser(message.getUser())
                .setDateTime(CURRENT_DATE_TIME.plusHours(1))
                .setTraining(getSomeTrainings().get(0))
                .setTrainSubscription(subscription);
        when(trainingEventService.get(user, eventId)).thenReturn(event);
        subscription.setCountLeft(ACTIVE_SUBSCRIPTION_COUNT_LEFT - 1);
        when(trainSubscriptionService.getFirstActive(message.getUser())).thenReturn(subscription);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse botResponse = training.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        assertNull(textResponse.getKeyboard());

        assertEquals(ACTIVE_SUBSCRIPTION_COUNT_LEFT, subscription.getCountLeft());
        verify(trainSubscriptionService).save(subscription);

        assertTrue(event.getCanceled());
        assertEquals(cancelReason, event.getCancellationReason());
        verify(trainingEventService).save(event);

        verify(commandWaitingService).remove(chat, user);
        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseReportEmptyCommandTest() {
        final String expectedResponseText = "<b>${command.training.choosereport}:</b>\n";
        BotRequest request = TestUtils.getRequestWithCallback("training_r");
        Message message = request.getMessage();

        PageImpl<TrainSubscription> trainSubscriptions = new PageImpl<>(List.of(getSomeActiveTrainingSubscription(), getSomeInactiveTrainingSubscription()));
        when(trainSubscriptionService.get(message.getUser(), 0)).thenReturn(trainSubscriptions);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertReportKeyboard(editResponse.getKeyboard(), false);

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseReportWithPageTest() {
        final int page = 2;
        final String expectedResponseText = "<b>${command.training.choosereport}:</b>\n";
        BotRequest request = TestUtils.getRequestWithCallback("training_r" + page);
        Message message = request.getMessage();

        PageImpl<TrainSubscription> trainSubscriptions = new PageImpl<>(
                List.of(getSomeActiveTrainingSubscription(), getSomeInactiveTrainingSubscription()),
                Pageable.ofSize(1),
                4);
        when(trainSubscriptionService.get(message.getUser(), page)).thenReturn(trainSubscriptions);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertReportKeyboard(editResponse.getKeyboard(), true);

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseReportCorruptedSubscriptionIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("training_r_suba");

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseReportNotExistenceSubscriptionTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("training_r_sub10");

        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseReportSubscriptionEventsNotExistsTest() {
        final String expectedResponseText = "${command.training.nostats}\n";
        BotRequest request = TestUtils.getRequestWithCallback("training_r_sub" + ACTIVE_SUBSCRIPTION_ID);
        Message message = request.getMessage();
        User user = message.getUser();

        TrainSubscription subscription = getSomeActiveTrainingSubscription();
        when(trainSubscriptionService.get(ACTIVE_SUBSCRIPTION_ID, user)).thenReturn(subscription);
        when(trainingEventService.getAll(user, subscription)).thenReturn(Collections.emptyList());
        PageImpl<TrainSubscription> trainSubscriptions = new PageImpl<>(List.of(getSomeActiveTrainingSubscription(), getSomeInactiveTrainingSubscription()));
        when(trainSubscriptionService.get(message.getUser(), 0)).thenReturn(trainSubscriptions);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertReportKeyboard(editResponse.getKeyboard(), "training_d_sub" + ACTIVE_SUBSCRIPTION_ID);

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseReportSubscriptionAllEventsAreCanceledTest() {
        final String expectedResponseText = "${command.training.notrainings}\n";
        BotRequest request = TestUtils.getRequestWithCallback("training_r_sub" + ACTIVE_SUBSCRIPTION_ID);
        Message message = request.getMessage();
        User user = message.getUser();

        TrainSubscription subscription = getSomeActiveTrainingSubscription();
        when(trainSubscriptionService.get(ACTIVE_SUBSCRIPTION_ID, user)).thenReturn(subscription);
        when(trainingEventService.getAll(user, subscription)).thenReturn(List.of(new TrainingEvent().setCanceled(true)));
        PageImpl<TrainSubscription> trainSubscriptions = new PageImpl<>(List.of(getSomeActiveTrainingSubscription(), getSomeInactiveTrainingSubscription()));
        when(trainSubscriptionService.get(message.getUser(), 0)).thenReturn(trainSubscriptions);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedResponseText, editResponse.getText());

        assertReportKeyboard(editResponse.getKeyboard(), "training_d_sub" + ACTIVE_SUBSCRIPTION_ID);

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseReportSubscriptionTest() {
        BotRequest request = TestUtils.getRequestWithCallback("training_r_sub" + ACTIVE_SUBSCRIPTION_ID);
        Message message = request.getMessage();
        User user = message.getUser();

        TrainSubscription subscription = getSomeActiveTrainingSubscription();
        when(trainSubscriptionService.get(ACTIVE_SUBSCRIPTION_ID, user)).thenReturn(subscription);
        when(trainingEventService.getAll(user, subscription)).thenReturn(getSomeTrainingEvents());
        PageImpl<TrainSubscription> trainSubscriptions = new PageImpl<>(List.of(getSomeActiveTrainingSubscription(), getSomeInactiveTrainingSubscription()));
        when(trainSubscriptionService.get(message.getUser(), 0)).thenReturn(trainSubscriptions);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(EXPECTED_REPORT, editResponse.getText());

        assertReportKeyboard(editResponse.getKeyboard(), "training_d_sub" + ACTIVE_SUBSCRIPTION_ID);

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseReportMonthTest() {
        BotRequest request = TestUtils.getRequestWithCallback("training_rm");
        Message message = request.getMessage();
        User user = message.getUser();

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(trainingEventService.getAllOfMonth(user, CURRENT_DATE_TIME.getMonth().getValue())).thenReturn(getSomeTrainingEvents());
        PageImpl<TrainSubscription> trainSubscriptions = new PageImpl<>(List.of(getSomeActiveTrainingSubscription(), getSomeInactiveTrainingSubscription()));
        when(trainSubscriptionService.get(message.getUser(), 0)).thenReturn(trainSubscriptions);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(EXPECTED_REPORT, editResponse.getText());

        assertReportKeyboard(editResponse.getKeyboard(), "training_dm");

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseReportYearTest() {
        BotRequest request = TestUtils.getRequestWithCallback("training_ry");
        Message message = request.getMessage();
        User user = message.getUser();

        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(trainingEventService.getAllOfYear(user, CURRENT_DATE_TIME.getYear())).thenReturn(getSomeTrainingEvents());
        PageImpl<TrainSubscription> trainSubscriptions = new PageImpl<>(List.of(getSomeActiveTrainingSubscription(), getSomeInactiveTrainingSubscription()));
        when(trainSubscriptionService.get(message.getUser(), 0)).thenReturn(trainSubscriptions);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(EXPECTED_REPORT, editResponse.getText());

        assertReportKeyboard(editResponse.getKeyboard(), "training_dy");

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseReportAllTimeTest() {
        BotRequest request = TestUtils.getRequestWithCallback("training_rall");
        Message message = request.getMessage();
        User user = message.getUser();

        when(trainingEventService.getAll(user)).thenReturn(getSomeTrainingEvents());
        PageImpl<TrainSubscription> trainSubscriptions = new PageImpl<>(List.of(getSomeActiveTrainingSubscription(), getSomeInactiveTrainingSubscription()));
        when(trainSubscriptionService.get(message.getUser(), 0)).thenReturn(trainSubscriptions);

        BotResponse botResponse = training.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(EXPECTED_REPORT, editResponse.getText());

        assertReportKeyboard(editResponse.getKeyboard(), "training_dall");

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseUnknownDownloadReportCommandTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("training_dx");
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn("en");
        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseDownloadReportCorruptedSubscriptionIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("training_d_suba");
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn("en");
        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseDownloadReportNotExistenceSubscriptionTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestWithCallback("training_d_sub123");
        Message message = request.getMessage();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn("en");
        when(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> training.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseDownloadReportSubscriptionTest() throws IOException {
        final String expectedResponseText = "${command.training.subscriptionreport} 25.12.1999 — 25.03.2000 (50)";
        final String expectedFileContent = "{command.training.report} 25.12.1999 — 25.03.2000 (50)\n\n" + EXPECTED_REPORT_FILE_CONTENT;
        BotRequest request = TestUtils.getRequestWithCallback("training_d_sub" + ACTIVE_SUBSCRIPTION_ID);
        Message message = request.getMessage();
        User user = message.getUser();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn("en");

        TrainSubscription subscription = getSomeActiveTrainingSubscription();
        when(trainSubscriptionService.get(ACTIVE_SUBSCRIPTION_ID, user)).thenReturn(subscription);
        when(trainingEventService.getAll(user, subscription)).thenReturn(getSomeTrainingEvents());
        when(internationalizationService.internationalize(anyString(), eq("en"))).thenAnswer(answer -> answer.getArgument(0));

        BotResponse botResponse = training.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);
        assertEquals(expectedResponseText, fileResponse.getText());
        String actualFileContent = new String(fileResponse.getFiles().get(0).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expectedFileContent, actualFileContent);

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseDownloadReportMonthTest() throws IOException {
        final String expectedResponseText = "{command.training.monthly} January";
        final String expectedFileContent = "{command.training.report} January\n\n" + EXPECTED_REPORT_FILE_CONTENT;
        BotRequest request = TestUtils.getRequestWithCallback("training_dm");
        Message message = request.getMessage();
        User user = message.getUser();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn("en");
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(languageResolver.getLocale(message.getChat())).thenReturn(Locale.ENGLISH);
        when(trainingEventService.getAllOfMonth(user, CURRENT_DATE_TIME.getMonth().getValue())).thenReturn(getSomeTrainingEvents());
        when(internationalizationService.internationalize(anyString(), eq("en"))).thenAnswer(answer -> answer.getArgument(0));

        BotResponse botResponse = training.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);
        assertEquals(expectedResponseText, fileResponse.getText());
        String actualFileContent = new String(fileResponse.getFiles().get(0).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expectedFileContent, actualFileContent);

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseDownloadReportYearTest() throws IOException {
        final String expectedResponseText = "{command.training.yearreport} 2000";
        final String expectedFileContent = "{command.training.report} 2000\n\n" + EXPECTED_REPORT_FILE_CONTENT;
        BotRequest request = TestUtils.getRequestWithCallback("training_dy");
        Message message = request.getMessage();
        User user = message.getUser();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn("en");
        when(clock.instant()).thenReturn(CURRENT_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(trainingEventService.getAllOfYear(user, CURRENT_DATE_TIME.getYear())).thenReturn(getSomeTrainingEvents());
        when(internationalizationService.internationalize(anyString(), eq("en"))).thenAnswer(answer -> answer.getArgument(0));

        BotResponse botResponse = training.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);
        assertEquals(expectedResponseText, fileResponse.getText());
        String actualFileContent = new String(fileResponse.getFiles().get(0).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expectedFileContent, actualFileContent);

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseDownloadReportAllTimeTest() throws IOException {
        final String expectedResponseText = "{command.training.alltimereport}";
        final String expectedFileContent = "{command.training.report} all\n\n" + EXPECTED_REPORT_FILE_CONTENT;
        BotRequest request = TestUtils.getRequestWithCallback("training_dall");
        Message message = request.getMessage();
        User user = message.getUser();

        when(languageResolver.getChatLanguageCode(message, message.getUser())).thenReturn("en");
        when(trainingEventService.getAll(user)).thenReturn(getSomeTrainingEvents());
        when(internationalizationService.internationalize(anyString(), eq("en"))).thenAnswer(answer -> answer.getArgument(0));

        BotResponse botResponse = training.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseParams(botResponse);
        assertEquals(expectedResponseText, fileResponse.getText());
        String actualFileContent = new String(fileResponse.getFiles().get(0).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expectedFileContent, actualFileContent);

        verify(bot).sendTyping(message.getChatId());
    }
    
    private List<TrainingEvent> getSomeTrainingEvents() {
        List<TrainingEvent> trainingEventList = new ArrayList<>();
        List<org.telegram.bot.domain.entities.Training> trainings = getSomeTrainings();
        TrainSubscription subscription = getSomeActiveTrainingSubscription();

        trainings
                .forEach(training1 -> LongStream.range(0, 5)
                        .mapToObj(index -> new TrainingEvent()
                                .setId(index)
                                .setDateTime(CURRENT_DATE_TIME.plusDays(index))
                                .setCanceled(false)
                                .setTraining(training1)
                                .setTrainSubscription(subscription))
                        .forEach(trainingEventList::add));

        trainings
                .forEach(training1 -> LongStream.range(0, 5)
                        .mapToObj(index -> new TrainingEvent()
                                .setId(index)
                                .setDateTime(CURRENT_DATE_TIME.plusDays(index))
                                .setCanceled(true)
                                .setCancellationReason("reason" + index)
                                .setTraining(training1)
                                .setTrainSubscription(subscription))
                        .forEach(trainingEventList::add));

        trainings
                .forEach(training1 -> LongStream.range(0, 5)
                        .mapToObj(index -> new TrainingEvent()
                                .setId(index)
                                .setDateTime(CURRENT_DATE_TIME.plusDays(index))
                                .setCanceled(false)
                                .setUnplanned(true)
                                .setTraining(training1)
                                .setTrainSubscription(subscription))
                        .forEach(trainingEventList::add));
        
        return trainingEventList;
    }

    private TrainSubscription getSomeInactiveTrainingSubscription() {
        return new TrainSubscription()
                .setId(INACTIVE_SUBSCRIPTION_ID)
                .setUser(TestUtils.getUser())
                .setCount(INACTIVE_SUBSCRIPTION_COUNT)
                .setCountLeft(INACTIVE_SUBSCRIPTION_COUNT_LEFT)
                .setStartDate(INACTIVE_SUBSCRIPTION_DATE_START)
                .setPeriod(INACTIVE_SUBSCRIPTION_PERIOD)
                .setActive(false);
    }

    private TrainSubscription getSomeActiveTrainingSubscription() {
        return new TrainSubscription()
                .setId(ACTIVE_SUBSCRIPTION_ID)
                .setUser(TestUtils.getUser())
                .setCount(ACTIVE_SUBSCRIPTION_COUNT)
                .setCountLeft(ACTIVE_SUBSCRIPTION_COUNT_LEFT)
                .setStartDate(ACTIVE_SUBSCRIPTION_DATE_START)
                .setPeriod(ACTIVE_SUBSCRIPTION_PERIOD)
                .setActive(true);
    }

    private List<org.telegram.bot.domain.entities.Training> getSomeTrainings() {
        return List.of(
                new org.telegram.bot.domain.entities.Training()
                        .setId(TRAINING1_ID)
                        .setUser(TestUtils.getUser())
                        .setTimeStart(TRAINING1_TIME_START)
                        .setTimeEnd(TRAINING1_TIME_END)
                        .setName(TRAINING1_NAME)
                        .setCost(TRAINING1_COST)
                        .setDeleted(false),
                new org.telegram.bot.domain.entities.Training()
                        .setId(TRAINING2_ID)
                        .setUser(TestUtils.getUser())
                        .setTimeStart(TRAINING2_TIME_START)
                        .setTimeEnd(TRAINING2_TIME_END)
                        .setName(TRAINING2_NAME)
                        .setCost(TRAINING2_COST)
                        .setDeleted(false),
                new org.telegram.bot.domain.entities.Training()
                        .setId(TRAINING3_ID)
                        .setUser(TestUtils.getUser())
                        .setTimeStart(TRAINING3_TIME_START)
                        .setTimeEnd(TRAINING3_TIME_END)
                        .setName(TRAINING3_NAME)
                        .setCost(TRAINING3_COST)
                        .setDeleted(false)
        );
    }

    private void assertMainKeyboard(Keyboard keyboard) {
        List<List<KeyboardButton>> keyboardButtonsList = keyboard.getKeyboardButtonsList();

        assertEquals(3, keyboardButtonsList.size());

        List<KeyboardButton> addRow = keyboardButtonsList.get(0);
        assertEquals(1, addRow.size());
        KeyboardButton addButton = addRow.get(0);
        assertEquals("\uD83C\uDD95${command.training.button.unplannedtraining}", addButton.getName());
        assertEquals("training_add", addButton.getCallback());

        List<KeyboardButton> reportsRow = keyboardButtonsList.get(1);
        assertEquals(1, reportsRow.size());
        KeyboardButton reportButton = reportsRow.get(0);
        assertEquals("\uD83D\uDCDD${command.training.button.reports}", reportButton.getName());
        assertEquals("training_r", reportButton.getCallback());

        List<KeyboardButton> setRow = keyboardButtonsList.get(2);
        assertEquals(1, setRow.size());
        KeyboardButton setButton = setRow.get(0);
        assertEquals("⚙\uFE0F${command.training.button.settings}", setButton.getName());
        assertEquals("${setter.command} ${setter.set.trainings}", setButton.getCallback());
    }

    private void assertReportKeyboard(Keyboard keyboard, String reportDownloadCommand) {
        assertReportKeyboard(keyboard, false, reportDownloadCommand);
    }

    private void assertReportKeyboard(Keyboard keyboard, boolean hasPages) {
        assertReportKeyboard(keyboard, hasPages, null);
    }

    private void assertReportKeyboard(Keyboard keyboard, boolean hasPages, String reportDownloadCommand) {
        List<List<KeyboardButton>> keyboardButtonsList = keyboard.getKeyboardButtonsList();

        int expectedKeyboardRowsSize;
        if (reportDownloadCommand != null) {
            expectedKeyboardRowsSize = 8;
        } else {
            expectedKeyboardRowsSize = 7;
        }
        assertEquals(expectedKeyboardRowsSize, keyboardButtonsList.size());
        
        int i = 0;
        
        List<KeyboardButton> firstSubscriptionRow = keyboardButtonsList.get(i);
        assertEquals(1, firstSubscriptionRow.size());
        KeyboardButton firstSubButton = firstSubscriptionRow.get(0);
        assertEquals(DateUtils.formatDate(ACTIVE_SUBSCRIPTION_DATE_START) + " — " + DateUtils.formatDate(ACTIVE_SUBSCRIPTION_DATE_START.plus(ACTIVE_SUBSCRIPTION_PERIOD)) + " (" + ACTIVE_SUBSCRIPTION_COUNT + ")\n", firstSubButton.getName());
        assertEquals("training_r_sub" + ACTIVE_SUBSCRIPTION_ID, firstSubButton.getCallback());
        i = i + 1;

        List<KeyboardButton> secondSubscriptionRow = keyboardButtonsList.get(i);
        assertEquals(1, secondSubscriptionRow.size());
        KeyboardButton secondSubButton = secondSubscriptionRow.get(0);
        assertEquals(DateUtils.formatDate(INACTIVE_SUBSCRIPTION_DATE_START) + " — " + DateUtils.formatDate(INACTIVE_SUBSCRIPTION_DATE_START.plus(INACTIVE_SUBSCRIPTION_PERIOD)) + " (" + INACTIVE_SUBSCRIPTION_COUNT + ")\n", secondSubButton.getName());
        assertEquals("training_r_sub" + INACTIVE_SUBSCRIPTION_ID, secondSubButton.getCallback());
        i = i + 1;

        List<KeyboardButton> pagesRow = keyboardButtonsList.get(i);
        if (hasPages) {
            assertEquals(2, pagesRow.size());

            KeyboardButton backButton = pagesRow.get(0);
            assertEquals("⬅\uFE0F${command.training.button.back}", backButton.getName());
            assertEquals("training_r1", backButton.getCallback());

            KeyboardButton forwardButton = pagesRow.get(1);
            assertEquals("${command.training.button.forward}➡\uFE0F", forwardButton.getName());
            assertEquals("training_r3", forwardButton.getCallback());
        } else {
            assertTrue(pagesRow.isEmpty());
        }
        i = i + 1;
        
        if (reportDownloadCommand != null) {
            List<KeyboardButton> downloadRow = keyboardButtonsList.get(i);
            assertEquals(1, downloadRow.size());

            KeyboardButton backButton = downloadRow.get(0);
            assertEquals("⬇\uFE0F${command.training.button.download}", backButton.getName());
            assertEquals(reportDownloadCommand, backButton.getCallback());
            i = i + 1;
        }

        List<KeyboardButton> currentMonthReportRow = keyboardButtonsList.get(i);
        assertEquals(1, currentMonthReportRow.size());
        KeyboardButton currentMonthButton = currentMonthReportRow.get(0);
        assertEquals("${command.training.button.forcurrentmonth}", currentMonthButton.getName());
        assertEquals("training_rm", currentMonthButton.getCallback());
        i = i + 1;

        List<KeyboardButton> currentYearReportRow = keyboardButtonsList.get(i);
        assertEquals(1, currentYearReportRow.size());
        KeyboardButton currentYearButton = currentYearReportRow.get(0);
        assertEquals("${command.training.button.forcurrentyear}", currentYearButton.getName());
        assertEquals("training_ry", currentYearButton.getCallback());
        i = i + 1;

        List<KeyboardButton> allTimeReportRow = keyboardButtonsList.get(i);
        assertEquals(1, allTimeReportRow.size());
        KeyboardButton allTimeButton = allTimeReportRow.get(0);
        assertEquals("${command.training.button.foralltime}", allTimeButton.getName());
        assertEquals("training_rall", allTimeButton.getCallback());
        i = i + 1;

        List<KeyboardButton> trainingsRow = keyboardButtonsList.get(i);
        assertEquals(1, trainingsRow.size());
        KeyboardButton trainingsButton = trainingsRow.get(0);
        assertEquals("\uD83C\uDFCB${command.training.button.trainings}", trainingsButton.getName());
        assertEquals("training", trainingsButton.getCallback());
    }

}