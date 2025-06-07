package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.TrackCode;
import org.telegram.bot.domain.entities.TrackCodeEvent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EditResponse;
import org.telegram.bot.domain.model.response.KeyboardButton;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParcelTest {

    private static final LocalDateTime SOME_DATE_TIME = LocalDateTime.of(2000, 1, 1, 1, 1);

    private static final String EXPECTED_ADD_BUTTON_NAME = "\uD83C\uDD95${command.parcel.button.add}";
    private static final String EXPECTED_ADD_BUTTON_CALLBACK = "parcel добавить";
    private static final String EXPECTED_UPDATE_BUTTON_NAME = "\uD83D\uDD04${command.parcel.button.reload}";
    private static final String EXPECTED_UPDATE_BUTTON_CALLBACK = "parcel ";
    private static final String EXPECTED_DELETE_FIRST_PARCEL_BUTTON_NAME = "❌parcel1 with very very very l...";
    private static final String EXPECTED_DELETE_FIRST_PARCEL_CALLBACK = "parcel удалить1";
    private static final String EXPECTED_DELETE_SECOND_PARCEL_BUTTON_NAME = "❌parcel2";
    private static final String EXPECTED_DELETE_SECOND_PARCEL_CALLBACK = "parcel удалить2";

    @Mock
    private ParcelService parcelService;
    @Mock
    private TrackCodeService trackCodeService;
    @Mock
    private UserService userService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private Bot bot;
    @Mock
    private BotStats botStats;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private Clock clock;

    @InjectMocks
    private Parcel parcel;

    @Test
    void parseCallbackEmptyTest() {
        final String expectedCaption = """
                <b>${command.parcel.caption}:</b>
                ${command.parcel.lastupdate}: <b>01.01 03:00</b>
                ${command.parcel.nextupdate}: <b>01.01 06:00</b>
                """;
        BotRequest request = TestUtils.getRequestWithCallback("parcel");

        BotResponse botResponse = parcel.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedCaption, editResponse.getText());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(2, keyboardButtonsList.size());

        List<KeyboardButton> addRow = keyboardButtonsList.get(0);
        assertEquals(1, addRow.size());
        KeyboardButton addButton = addRow.get(0);
        assertEquals(EXPECTED_ADD_BUTTON_NAME, addButton.getName());
        assertEquals(EXPECTED_ADD_BUTTON_CALLBACK, addButton.getCallback());

        List<KeyboardButton> updateRow = keyboardButtonsList.get(1);
        assertEquals(1, updateRow.size());
        KeyboardButton updateButton = updateRow.get(0);
        assertEquals(EXPECTED_UPDATE_BUTTON_NAME, updateButton.getName());
        assertEquals(EXPECTED_UPDATE_BUTTON_CALLBACK, updateButton.getCallback());
    }

    @Test
    void parseCallbackEmptyWithStoredDataTest() {
        final String expectedCaption = """
                <b>${command.parcel.caption}:</b>
                <code>barcode1</code> — <b><a href="https://www.pochta.ru/tracking?barcode=barcode1">parcel1 with very very very long name</a></b>
                ${command.parcel.noinformation}
                -----------------------------
                <code>barcode2</code> — <b><a href="https://www.pochta.ru/tracking?barcode=barcode2">parcel2</a></b>
                <u>item_name2</u> 124 г.
                <i>01.01.2000 01:05:00</i>
                <b>operationType2</b> operationDescription2
                address2 (100002)
                /parcel_2
                -----------------------------
                ${command.parcel.lastupdate}: <b>01.01 03:00</b>
                ${command.parcel.nextupdate}: <b>01.01 06:00</b>
                """;

        BotRequest request = TestUtils.getRequestWithCallback("parcel");

        when(parcelService.get(request.getMessage().getUser())).thenReturn(getSomeStoredParcels());

        BotResponse botResponse = parcel.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedCaption, editResponse.getText());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(4, keyboardButtonsList.size());

        List<KeyboardButton> deleteFirstParcelRow = keyboardButtonsList.get(0);
        assertEquals(1, deleteFirstParcelRow.size());
        KeyboardButton deleteFirstParcelButton = deleteFirstParcelRow.get(0);
        assertEquals(EXPECTED_DELETE_FIRST_PARCEL_BUTTON_NAME, deleteFirstParcelButton.getName());
        assertEquals(EXPECTED_DELETE_FIRST_PARCEL_CALLBACK, deleteFirstParcelButton.getCallback());

        List<KeyboardButton> deleteSecondParcelRow = keyboardButtonsList.get(1);
        assertEquals(1, deleteSecondParcelRow.size());
        KeyboardButton deleteSecondParcelButton = deleteSecondParcelRow.get(0);
        assertEquals(EXPECTED_DELETE_SECOND_PARCEL_BUTTON_NAME, deleteSecondParcelButton.getName());
        assertEquals(EXPECTED_DELETE_SECOND_PARCEL_CALLBACK, deleteSecondParcelButton.getCallback());

        List<KeyboardButton> addRow = keyboardButtonsList.get(2);
        assertEquals(1, addRow.size());
        KeyboardButton addButton = addRow.get(0);
        assertEquals(EXPECTED_ADD_BUTTON_NAME, addButton.getName());
        assertEquals(EXPECTED_ADD_BUTTON_CALLBACK, addButton.getCallback());

        List<KeyboardButton> updateRow = keyboardButtonsList.get(3);
        assertEquals(1, updateRow.size());
        KeyboardButton updateButton = updateRow.get(0);
        assertEquals(EXPECTED_UPDATE_BUTTON_NAME, updateButton.getName());
        assertEquals(EXPECTED_UPDATE_BUTTON_CALLBACK, updateButton.getCallback());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a"})
    void parseCallbackDeleteEmptyArgumentTest(String id) {
        BotRequest request = TestUtils.getRequestWithCallback("parcel удалить" + id);
        assertThrows((BotException.class), () -> parcel.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void parseCallbackDeleteNotExistenceEntityIdAsArgumentTest() {
        BotRequest request = TestUtils.getRequestWithCallback("parcel удалить123");
        assertThrows((BotException.class), () -> parcel.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
    }

    @Test
    void parseCallbackDeleteSomeOneElseEntityIdAsArgumentTest() {
        final Long parcelId = 123L;
        BotRequest request = TestUtils.getRequestWithCallback("parcel удалить" + parcelId);
        org.telegram.bot.domain.entities.Parcel parcelWithoutEvents = getParcelWithoutEvents();
        parcelWithoutEvents.setUser(TestUtils.getUser(TestUtils.ANOTHER_USER_ID));

        when(parcelService.get(request.getMessage().getUser(), parcelId)).thenReturn(parcelWithoutEvents);

        assertThrows((BotException.class), () -> parcel.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.NOT_OWNER);
    }

    @Test
    void parseCallbackDeleteEntityIdAsArgumentTest() {
        final String expectedCaption = """
                <b>${command.parcel.caption}:</b>
                <code>barcode1</code> — <b><a href="https://www.pochta.ru/tracking?barcode=barcode1">parcel1 with very very very long name</a></b>
                ${command.parcel.noinformation}
                -----------------------------
                <code>barcode2</code> — <b><a href="https://www.pochta.ru/tracking?barcode=barcode2">parcel2</a></b>
                <u>item_name2</u> 124 г.
                <i>01.01.2000 01:05:00</i>
                <b>operationType2</b> operationDescription2
                address2 (100002)
                /parcel_2
                -----------------------------
                ${command.parcel.lastupdate}: <b>01.01 03:00</b>
                ${command.parcel.nextupdate}: <b>01.01 06:00</b>
                """;
        final Long parcelId = 123L;
        BotRequest request = TestUtils.getRequestWithCallback("parcel удалить" + parcelId);
        org.telegram.bot.domain.entities.Parcel parcelWithoutEvents = getParcelWithoutEvents();

        when(parcelService.get(request.getMessage().getUser(), parcelId)).thenReturn(parcelWithoutEvents);
        when(parcelService.get(request.getMessage().getUser())).thenReturn(getSomeStoredParcels());

        BotResponse botResponse = parcel.parse(request).get(0);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(botResponse);
        assertEquals(expectedCaption, editResponse.getText());

        List<List<KeyboardButton>> keyboardButtonsList = editResponse.getKeyboard().getKeyboardButtonsList();
        assertEquals(4, keyboardButtonsList.size());

        List<KeyboardButton> deleteFirstParcelRow = keyboardButtonsList.get(0);
        assertEquals(1, deleteFirstParcelRow.size());
        KeyboardButton deleteFirstParcelButton = deleteFirstParcelRow.get(0);
        assertEquals(EXPECTED_DELETE_FIRST_PARCEL_BUTTON_NAME, deleteFirstParcelButton.getName());
        assertEquals(EXPECTED_DELETE_FIRST_PARCEL_CALLBACK, deleteFirstParcelButton.getCallback());

        List<KeyboardButton> deleteSecondParcelRow = keyboardButtonsList.get(1);
        assertEquals(1, deleteSecondParcelRow.size());
        KeyboardButton deleteSecondParcelButton = deleteSecondParcelRow.get(0);
        assertEquals(EXPECTED_DELETE_SECOND_PARCEL_BUTTON_NAME, deleteSecondParcelButton.getName());
        assertEquals(EXPECTED_DELETE_SECOND_PARCEL_CALLBACK, deleteSecondParcelButton.getCallback());

        List<KeyboardButton> addRow = keyboardButtonsList.get(2);
        assertEquals(1, addRow.size());
        KeyboardButton addButton = addRow.get(0);
        assertEquals(EXPECTED_ADD_BUTTON_NAME, addButton.getName());
        assertEquals(EXPECTED_ADD_BUTTON_CALLBACK, addButton.getCallback());

        List<KeyboardButton> updateRow = keyboardButtonsList.get(3);
        assertEquals(1, updateRow.size());
        KeyboardButton updateButton = updateRow.get(0);
        assertEquals(EXPECTED_UPDATE_BUTTON_NAME, updateButton.getName());
        assertEquals(EXPECTED_UPDATE_BUTTON_CALLBACK, updateButton.getCallback());
    }

    @Test
    void parseCallbackAddParcelTest() {
        final String expectedResponseText = "${command.parcel.parceladdinghint}: <code>${command.parcel.parceladdingexample}</code>";
        BotRequest request = TestUtils.getRequestWithCallback("parcel добавить");
        BotResponse botResponse = parcel.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        Message message = request.getMessage();
        verify(commandWaitingService).get(message.getChat(), message.getUser());
        verify(commandWaitingService).add(message.getChat(), message.getUser(), Parcel.class, "parcel добавить");
    }

    @Test
    void addParcelWithoutFreeSlotsTest() {
        final String expectedErrorMessage = "${command.parcel.noslots}";
        BotRequest request = TestUtils.getRequestFromGroup("trackcode");
        Message message = request.getMessage();
        CommandWaiting commandWaiting = new CommandWaiting()
                .setTextMessage("parcel добавить ");

        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(trackCodeService.getTrackCodesCount()).thenReturn(13L);
        when(propertiesConfig.getRussianPostRequestsLimit()).thenReturn(100);

        BotException botException = assertThrows((BotException.class), () -> parcel.parse(request));
        assertEquals(expectedErrorMessage, botException.getMessage());

        verify(commandWaitingService).remove(commandWaiting);
    }

    @Test
    void addParcelAlreadyExistenceTest() {
        final String trackcode = "trackcode";
        BotRequest request = TestUtils.getRequestFromGroup(trackcode);
        Message message = request.getMessage();
        CommandWaiting commandWaiting = new CommandWaiting()
                .setTextMessage("parcel добавить ");

        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(trackCodeService.getTrackCodesCount()).thenReturn(10L);
        when(propertiesConfig.getRussianPostRequestsLimit()).thenReturn(100);
        when(parcelService.getByBarcode(request.getMessage().getUser(), trackcode)).thenReturn(getParcelWithoutEvents());

        assertThrows((BotException.class), () -> parcel.parse(request));

        verify(commandWaitingService).remove(commandWaiting);
        verify(speechService).getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY);
    }

    @Test
    void addParcelWithNameTest() {
        final String parcelName = "name";
        final String trackcode = "trackcode";
        BotRequest request = TestUtils.getRequestFromGroup(parcelName + " " + trackcode);
        Message message = request.getMessage();
        CommandWaiting commandWaiting = new CommandWaiting()
                .setTextMessage("parcel добавить ");

        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(trackCodeService.getTrackCodesCount()).thenReturn(10L);
        when(trackCodeService.save(any(TrackCode.class))).then(answer -> answer.getArgument(0));
        when(propertiesConfig.getRussianPostRequestsLimit()).thenReturn(100);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        BotResponse botResponse = parcel.parse(request).get(0);
        TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(speechService).getRandomMessageByTag(BotSpeechTag.SAVED);

        verify(parcelService).getByBarcodeOrName(message.getUser(), parcelName, trackcode);

        ArgumentCaptor<TrackCode> trackCodeArgumentCaptor = ArgumentCaptor.forClass(TrackCode.class);
        verify(trackCodeService).save(trackCodeArgumentCaptor.capture());
        assertEquals(trackcode, trackCodeArgumentCaptor.getValue().getBarcode());

        ArgumentCaptor<org.telegram.bot.domain.entities.Parcel> parcelArgumentCaptor = ArgumentCaptor.forClass(org.telegram.bot.domain.entities.Parcel.class);
        verify(parcelService).save(parcelArgumentCaptor.capture());
        org.telegram.bot.domain.entities.Parcel storedParcel = parcelArgumentCaptor.getValue();
        assertEquals(message.getUser(), storedParcel.getUser());
        assertEquals(parcelName, storedParcel.getName());
        assertEquals(trackcode, storedParcel.getTrackCode().getBarcode());
    }

    @Test
    void addParcelWithoutNameTest() {
        final String trackcode = "trackcode";
        BotRequest request = TestUtils.getRequestFromGroup(trackcode);
        Message message = request.getMessage();
        CommandWaiting commandWaiting = new CommandWaiting()
                .setTextMessage("parcel добавить ");

        when(commandWaitingService.get(message.getChat(), message.getUser())).thenReturn(commandWaiting);
        when(trackCodeService.getTrackCodesCount()).thenReturn(10L);
        when(trackCodeService.get(trackcode)).thenReturn(new TrackCode().setBarcode(trackcode));
        when(propertiesConfig.getRussianPostRequestsLimit()).thenReturn(100);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        BotResponse botResponse = parcel.parse(request).get(0);
        TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(speechService).getRandomMessageByTag(BotSpeechTag.SAVED);

        verify(parcelService).getByBarcode(message.getUser(), trackcode);

        ArgumentCaptor<org.telegram.bot.domain.entities.Parcel> parcelArgumentCaptor = ArgumentCaptor.forClass(org.telegram.bot.domain.entities.Parcel.class);
        verify(parcelService).save(parcelArgumentCaptor.capture());
        org.telegram.bot.domain.entities.Parcel storedParcel = parcelArgumentCaptor.getValue();
        assertEquals(message.getUser(), storedParcel.getUser());
        assertEquals(trackcode, storedParcel.getName());
        assertEquals(trackcode, storedParcel.getTrackCode().getBarcode());
    }

    @Test
    void deleteParcelWrongArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("parcel удалить ");

        assertThrows((BotException.class), () -> parcel.parse(request).get(0));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(parcelService, never()).getByName(any(User.class), anyString());
        verify(parcelService, never()).remove(any(org.telegram.bot.domain.entities.Parcel.class));
    }

    @Test
    void deleteUnknownParcelTest() {
        BotRequest request = TestUtils.getRequestFromGroup("parcel удалить test");

        assertThrows((BotException.class), () -> parcel.parse(request).get(0));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(parcelService, never()).remove(any(org.telegram.bot.domain.entities.Parcel.class));
    }

    @Test
    void deleteParcelByTrackcodeTest() {
        final String trackcode = "trackcode";
        BotRequest request = TestUtils.getRequestFromGroup("parcel удалить " + trackcode);
        Message message = request.getMessage();
        org.telegram.bot.domain.entities.Parcel parcelEntity = new org.telegram.bot.domain.entities.Parcel();

        when(parcelService.getByBarcode(message.getUser(), trackcode)).thenReturn(parcelEntity);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        BotResponse botResponse = parcel.parse(request).get(0);
        TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(speechService).getRandomMessageByTag(BotSpeechTag.SAVED);
        verify(parcelService).remove(parcelEntity);
    }

    @Test
    void deleteParcelByNameTest() {
        final String parcelName = "parcelName";
        BotRequest request = TestUtils.getRequestFromGroup("parcel удалить " + parcelName);
        Message message = request.getMessage();
        org.telegram.bot.domain.entities.Parcel parcelEntity = new org.telegram.bot.domain.entities.Parcel();

        when(parcelService.getByName(message.getUser(), parcelName)).thenReturn(parcelEntity);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        BotResponse botResponse = parcel.parse(request).get(0);
        TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(speechService).getRandomMessageByTag(BotSpeechTag.SAVED);
        verify(parcelService).remove(parcelEntity);
    }

    @Test
    void deleteByWrongIdTest() {
        BotRequest request = TestUtils.getRequestFromGroup("parcel_da");

        assertThrows((BotException.class), () -> parcel.parse(request).get(0));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(parcelService, never()).remove(any(org.telegram.bot.domain.entities.Parcel.class));
    }

    @Test
    void deleteByIdUnknownParcelTest() {
        BotRequest request = TestUtils.getRequestFromGroup("parcel_d1");

        assertThrows((BotException.class), () -> parcel.parse(request).get(0));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(parcelService, never()).remove(any(org.telegram.bot.domain.entities.Parcel.class));
    }

    @Test
    void deleteByIdTest() {
        final Long parcelId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("parcel_d" + parcelId);
        Message message = request.getMessage();
        org.telegram.bot.domain.entities.Parcel parcelEntity = new org.telegram.bot.domain.entities.Parcel();

        when(parcelService.get(message.getUser(), parcelId)).thenReturn(parcelEntity);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn("saved");

        BotResponse botResponse = parcel.parse(request).get(0);
        TestUtils.checkDefaultTextResponseParams(botResponse);

        verify(speechService).getRandomMessageByTag(BotSpeechTag.SAVED);
        verify(parcelService).remove(parcelEntity);
    }

    @Test
    void getTrackDataFoundNothingTest() {
        BotRequest request = TestUtils.getRequestFromGroup("parcel tratatam");
        assertThrows((BotException.class), () -> parcel.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void getTrackDataByIdEconomyModeTest() {
        final String expectedResponse = """
                <code>barcode1</code>
                <a href="https://www.pochta.ru/tracking?barcode=barcode1">${command.parcel.trackinfo.checkonwebsite}</a>
                -----------------------------
                ${command.parcel.lastupdate}: <b>01.01 03:00</b>
                ${command.parcel.nextupdate}: <b>01.01 06:00</b>
                """;
        final long parcelId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("parcel_" + parcelId);
        Message message = request.getMessage();
        org.telegram.bot.domain.entities.Parcel parcelEntity = getParcelWithoutEvents();

        when(parcelService.get(message.getUser(), parcelId)).thenReturn(parcelEntity);
        when(propertiesConfig.getRussianPostRequestsLimit()).thenReturn(5);

        BotResponse botResponse = parcel.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());

        verify(trackCodeService, never()).updateFromApi(any(TrackCode.class));
    }

    @Test
    void getTrackDataByNameAdminRequestTest() {
        final String expectedResponse = """
                <u>item_name1</u> 123 г.
                <i>01.01.2000 01:04:00</i>
                <b>operationType1</b> operationDescription1
                address1 (100001)
                -----------------------------
                <u>item_name2</u> 124 г.
                <i>01.01.2000 01:05:00</i>
                <b>operationType2</b> operationDescription2
                address2 (100002)
                -----------------------------

                <i>01.01.2000 01:11:00</i>


                -----------------------------
                <code>barcode2</code>
                ${command.parcel.trackinfo.register}:\s
                <b>01.01.2000 01:04:00</b>
                ${command.parcel.trackinfo.daysway}: <b>3 ${utils.date.m}. </b>
                ${command.parcel.trackinfo.fromcountry}: <b>country_from1</b>
                ${command.parcel.trackinfo.tocountry}: <b>country_to1</b>
                ${command.parcel.trackinfo.sender}: <b>sender1</b>
                ${command.parcel.trackinfo.receiver}: <b>recipient1</b>
                <a href="https://www.pochta.ru/tracking?barcode=barcode2">${command.parcel.trackinfo.checkonwebsite}</a>
                -----------------------------
                ${command.parcel.lastupdate}: <b>01.01 01:01</b>
                ${command.parcel.nextupdate}: <b>01.01 06:00</b>
                """;
        final String parcelName = "test";
        BotRequest request = TestUtils.getRequestFromGroup("parcel " + parcelName);
        Message message = request.getMessage();
        org.telegram.bot.domain.entities.Parcel parcelEntity = getParcelWithEvents();

        when(clock.instant()).thenReturn(SOME_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(parcelService.getByBarcodeOrName(message.getUser(), parcelName)).thenReturn(parcelEntity);
        when(propertiesConfig.getRussianPostRequestsLimit()).thenReturn(5);
        when(userService.getUserAccessLevel(parcelEntity.getUser().getUserId())).thenReturn(AccessLevel.ADMIN.getValue());
        when(parcelService.getAll(parcelEntity.getTrackCode())).thenReturn(List.of(getParcelWithEvents()));
        doAnswer(invocationOnMock -> parcelEntity.getTrackCode().getEvents().add(new TrackCodeEvent().setEventDateTime(SOME_DATE_TIME.plusMinutes(10))))
                .when(trackCodeService).updateFromApi(parcelEntity.getTrackCode());

        BotResponse botResponse = parcel.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());

        verify(trackCodeService).updateFromApi(any(TrackCode.class));
    }

    @Test
    void getTrackDataByIdNotEconomyModeTest() {
        final String expectedResponse = """
                <u>item_name1</u> 123 г.
                <i>01.01.2000 01:04:00</i>
                <b>operationType1</b> operationDescription1
                address1 (100001)
                -----------------------------
                <u>item_name2</u> 124 г.
                <i>01.01.2000 01:05:00</i>
                <b>operationType2</b> operationDescription2
                address2 (100002)
                -----------------------------

                <i>01.01.2000 01:11:00</i>


                -----------------------------
                <code>barcode2</code>
                ${command.parcel.trackinfo.register}:\s
                <b>01.01.2000 01:04:00</b>
                ${command.parcel.trackinfo.daysway}: <b>3 ${utils.date.m}. </b>
                ${command.parcel.trackinfo.fromcountry}: <b>country_from1</b>
                ${command.parcel.trackinfo.tocountry}: <b>country_to1</b>
                ${command.parcel.trackinfo.sender}: <b>sender1</b>
                ${command.parcel.trackinfo.receiver}: <b>recipient1</b>
                <a href="https://www.pochta.ru/tracking?barcode=barcode2">${command.parcel.trackinfo.checkonwebsite}</a>
                -----------------------------
                ${command.parcel.lastupdate}: <b>01.01 01:01</b>
                ${command.parcel.nextupdate}: <b>01.01 06:00</b>
                """;
        final String notifyUserText = """
                <b>parcel2</b>
                <code>barcode2</code>

                <i>01.01.2000 01:11:00</i>


                /parcel_2
                """;
        final long parcelId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("parcel_" + parcelId);
        Message message = request.getMessage();
        message.setUser(TestUtils.getUser(TestUtils.ANOTHER_USER_ID));
        org.telegram.bot.domain.entities.Parcel parcelEntity = getParcelWithEvents();

        when(clock.instant()).thenReturn(SOME_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(parcelService.get(message.getUser(), parcelId)).thenReturn(parcelEntity);
        when(propertiesConfig.getRussianPostRequestsLimit()).thenReturn(500);
        when(parcelService.getAll(parcelEntity.getTrackCode())).thenReturn(List.of(getParcelWithEvents()));
        doAnswer(invocationOnMock -> parcelEntity.getTrackCode().getEvents().add(new TrackCodeEvent().setEventDateTime(SOME_DATE_TIME.plusMinutes(10))))
                .when(trackCodeService).updateFromApi(parcelEntity.getTrackCode());

        BotResponse botResponse = parcel.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);

        assertEquals(expectedResponse, textResponse.getText());

        verify(trackCodeService).updateFromApi(any(TrackCode.class));

        ArgumentCaptor<TextResponse> notifyUserTextResponse = ArgumentCaptor.forClass(TextResponse.class);
        verify(bot).sendMessage(notifyUserTextResponse.capture());

        TextResponse userNotifyTextResponse = notifyUserTextResponse.getValue();
        TestUtils.checkDefaultTextResponseParams(userNotifyTextResponse);

        assertEquals(notifyUserText, userNotifyTextResponse.getText());
    }

    private List<org.telegram.bot.domain.entities.Parcel> getSomeStoredParcels() {
        return List.of(getParcelWithoutEvents(), getParcelWithEvents());
    }

    private org.telegram.bot.domain.entities.Parcel getParcelWithoutEvents() {
        return new org.telegram.bot.domain.entities.Parcel()
                .setId(1L)
                .setUser(TestUtils.getUser())
                .setName("parcel1 with very very very long name")
                .setTrackCode(new TrackCode()
                        .setId(10L)
                        .setBarcode("barcode1")
                        .setCreateDateTime(SOME_DATE_TIME.plusMinutes(1))
                        .setEvents(Set.of()));
    }

    private org.telegram.bot.domain.entities.Parcel getParcelWithEvents() {
        return new org.telegram.bot.domain.entities.Parcel()
                .setId(2L)
                .setUser(TestUtils.getUser())
                .setName("parcel2")
                .setTrackCode(new TrackCode()
                        .setId(11L)
                        .setBarcode("barcode2")
                        .setCreateDateTime(SOME_DATE_TIME.plusMinutes(2))
                        .setEvents(getCodeEventList()));
    }

    private Set<TrackCodeEvent> getCodeEventList() {
        return new HashSet<>(Set.of(
                new TrackCodeEvent()
                        .setId(100L)
                        .setEventBarcode("barcode2")
                        .setEventDateTime(SOME_DATE_TIME.plusMinutes(3))
                        .setAddress("address1")
                        .setIndex("100001")
                        .setItemName("item_name1")
                        .setGram(123L)
                        .setOperationType("operationType1")
                        .setOperationDescription("operationDescription1")
                        .setCountryFrom("country_from1")
                        .setCountryTo("country_to1")
                        .setSender("sender1")
                        .setRecipient("recipient1"),
                new TrackCodeEvent()
                        .setId(101L)
                        .setEventBarcode("barcode2")
                        .setEventDateTime(SOME_DATE_TIME.plusMinutes(4))
                        .setAddress("address2")
                        .setIndex("100002")
                        .setItemName("item_name2")
                        .setGram(124L)
                        .setOperationType("operationType2")
                        .setOperationDescription("operationDescription2")
                        .setCountryFrom("country_from2")
                        .setCountryTo("country_to2")
                        .setSender("sender2")
                        .setRecipient("recipient2")));
    }

}