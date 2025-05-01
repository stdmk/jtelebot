package org.telegram.bot.commands.setters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.DateUtils;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitySetterTest {

    @Mock
    private CityService cityService;
    @Mock
    private UserCityService userCityService;
    @Mock
    private SpeechService speechService;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private InternationalizationService internationalizationService;

    @InjectMocks
    private CitySetter citySetter;

    @BeforeEach
    void init() {
        when(internationalizationService.getAllTranslations("setter.city.emptycommand")).thenReturn(Set.of("city"));
        when(internationalizationService.internationalize("${setter.city.emptycommand} ${setter.city.update}")).thenReturn(Set.of("city update"));
        when(internationalizationService.internationalize("${setter.city.emptycommand} ${setter.city.remove}")).thenReturn(Set.of("city remove"));
        when(internationalizationService.internationalize("${setter.city.emptycommand} ${setter.city.add}")).thenReturn(Set.of("city add"));
        when(internationalizationService.internationalize( "${setter.city.emptycommand} ${setter.city.select}")).thenReturn(Set.of("city select"));
        when(internationalizationService.internationalize("${setter.city.emptycommand} ${setter.city.zone}")).thenReturn(Set.of("city zone"));

        ReflectionTestUtils.invokeMethod(citySetter, "postConstruct");
    }

    @Test
    void canProcessedTest() {
        assertFalse(citySetter.canProcessed(""));
        assertFalse(citySetter.canProcessed(" "));
        assertFalse(citySetter.canProcessed("tratatam"));
        assertFalse(citySetter.canProcessed("cit"));
        assertTrue(citySetter.canProcessed("city"));
    }

    @Test
    void getAccessLevelTest() {
        assertEquals(AccessLevel.TRUSTED, citySetter.getAccessLevel());
    }

    @Test
    void setWithUnknownCommandTest() {
        final String expectedErrorText = "error";
        final String argument = "test";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"city", "city update"})
    void setCallbackWithoutArgumentsTest(String argument) {
        final String expectedResponseText = "${setter.city.citynotset}. ${setter.city.pushbutton} \"${setter.city.button.select}\"";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        BotResponse response = citySetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertMainKeyboard(editResponse.getKeyboard());
    }

    @Test
    void setCallbackSelectCityEmptyArgsTest() {
        final String expectedResponseText = "${setter.city.selectoradd}";
        final String argument = "city select";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        List<City> cities = getSomeCities(request.getMessage().getUser());
        when(cityService.getAll()).thenReturn(cities);

        BotResponse response = citySetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertSelectKeyboard(editResponse.getKeyboard(), cities);
    }

    @Test
    void setCallbackSelectCityWrongIdTest() {
        final String expectedErrorText = "error";
        final String argument = "city select id";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackSelectCityNotExistenceIdTest() {
        final String expectedErrorText = "error";
        final String argument = "city select 0";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackSelectCityFirstTimeTest() {
        final String expectedResponseText = "${setter.city.selectedcity}: Тест1 (GMT+01:00)";
        final long cityId = 1L;
        final String argument = "city select " + cityId;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        City city = getSomeCities(user).get(0);
        when(cityService.get(cityId)).thenReturn(city);
        UserCity storedUserCity = new UserCity().setId(1L).setChat(chat).setUser(user).setCity(city);
        when(userCityService.get(user, chat)).thenReturn(null).thenReturn(storedUserCity);

        BotResponse response = citySetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertMainKeyboard(editResponse.getKeyboard());

        ArgumentCaptor<UserCity> userCityArgumentCaptor = ArgumentCaptor.forClass(UserCity.class);
        verify(userCityService).save(userCityArgumentCaptor.capture());

        UserCity userCity = userCityArgumentCaptor.getValue();
        assertEquals(chat, userCity.getChat());
        assertEquals(user, userCity.getUser());
        assertEquals(city, userCity.getCity());
    }

    @Test
    void setCallbackSelectCityTest() {
        final String expectedResponseText = "${setter.city.selectedcity}: Тест1 (GMT+01:00)";
        final long cityId = 1L;
        final String argument = "city select " + cityId;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        City city = getSomeCities(user).get(0);
        when(cityService.get(cityId)).thenReturn(city);
        UserCity storedUserCity = new UserCity().setId(1L).setChat(chat).setUser(user).setCity(city);
        when(userCityService.get(user, chat)).thenReturn(storedUserCity).thenReturn(storedUserCity);

        BotResponse response = citySetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertMainKeyboard(editResponse.getKeyboard());

        ArgumentCaptor<UserCity> userCityArgumentCaptor = ArgumentCaptor.forClass(UserCity.class);
        verify(userCityService).save(userCityArgumentCaptor.capture());

        UserCity userCity = userCityArgumentCaptor.getValue();
        assertEquals(chat, userCity.getChat());
        assertEquals(user, userCity.getUser());
        assertEquals(city, userCity.getCity());
    }

    @Test
    void setCallbackDeleteCityEmptyArgsTest() {
        final String expectedResponseText = "${setter.city.owncitiescaption}";
        final String argument = "city remove";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        List<City> cities = getSomeCities(request.getMessage().getUser());
        when(cityService.getAll(request.getMessage().getUser())).thenReturn(cities);

        BotResponse response = citySetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertRemoveKeyboard(editResponse.getKeyboard(), cities);
    }

    @Test
    void setCallbackRemoveCityWrongIdTest() {
        final String expectedErrorText = "error";
        final String argument = "city remove id";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackRemoveCityNotExistenceIdTest() {
        final String expectedErrorText = "error";
        final String argument = "city remove 0";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackRemoveUsedCityTest() {
        final String expectedErrorText = "error";
        final long cityId = 1L;
        final String argument = "city remove " + cityId;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        City city = getSomeCities(request.getMessage().getUser()).get(0);
        when(cityService.get(cityId)).thenReturn(city);
        when(userCityService.getAll(city)).thenReturn(List.of(new UserCity()));
        when(speechService.getRandomMessageByTag(BotSpeechTag.DATA_BASE_INTEGRITY)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackDeleteCityTest() {
        final String expectedResponseText = "${setter.city.owncitiescaption}";
        final long cityId = 1L;
        final String argument = "city remove " + cityId;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        User user = request.getMessage().getUser();

        List<City> cities = getSomeCities(user);
        City city = cities.get(0);
        when(cityService.get(cityId)).thenReturn(city);
        when(cityService.getAll(user)).thenReturn(cities);

        BotResponse response = citySetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertRemoveKeyboard(editResponse.getKeyboard(), cities);

        verify(cityService).remove(city);
    }

    @Test
    void setCallbackAddCityTest() {
        final String expectedResponseText = "\n" +
                "${setter.city.commandwaitingstart}";
        final String argument = "city add";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);
        Message message = request.getMessage();

        BotResponse response = citySetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        verify(commandWaitingService).add(message.getChat(), message.getUser(), org.telegram.bot.commands.Set.class,  "${setter.command} ${setter.city.emptycommand} ${setter.city.add}");
    }

    @Test
    void setCallbackTimezoneEmptyArgsTest() {
        final String expectedErrorText = "";
        final String argument = "city zone";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackTimezoneOnlyOneArgTest() {
        final String expectedErrorText = "error";
        final String argument = "city zone 0";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackTimezoneWrongArgsTest() {
        final String expectedErrorText = "";
        final String argument = "city zone a test";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackTimezoneNotOwnerCityTest() {
        final String expectedErrorText = "";
        final long cityId = 1L;
        final String argument = "city zone " + cityId + " GMT+05:00";
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        when(cityService.get(cityId)).thenReturn(new City().setUser(new User().setUserId(123L)));
        when(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setCallbackTimezoneTest() {
        final String expectedResponseText = "${setter.city.citynotset}. ${setter.city.pushbutton} \"${setter.city.button.select}\"";
        final long cityId = 1L;
        final String timezone = "GMT+05:00";
        final String argument = "city zone " + cityId + " " + timezone;
        BotRequest request = TestUtils.getRequestWithCallback("set " + argument);

        City city = getSomeCities(request.getMessage().getUser()).get(0);
        when(cityService.get(cityId)).thenReturn(city);

        BotResponse response = citySetter.set(request, argument);

        EditResponse editResponse = TestUtils.checkDefaultEditResponseParams(response);
        assertEquals(expectedResponseText, editResponse.getText());

        assertMainKeyboard(editResponse.getKeyboard());

        assertEquals(TimeZone.getTimeZone(timezone).getID(), city.getTimeZone());
    }

    @Test
    void setWithoutArgumentsTest() {
        final String expectedResponseText = "${setter.city.citynotset}. ${setter.city.pushbutton} \"${setter.city.button.select}\"";
        final String argument = "city";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        BotResponse response = citySetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        assertMainKeyboard(textResponse.getKeyboard());
    }

    @Test
    void setDeleteCityEmptyArgsTest() {
        final String expectedErrorText = "error";
        final String argument = "city remove";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setDeleteCityNotOwnerTest() {
        final String expectedErrorText = "error";
        final String cityName = "name";
        final String argument = "city remove " + cityName;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        when(cityService.get(cityName)).thenReturn(new City().setUser(new User().setUserId(123L)));
        when(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void setDeleteCityTest() {
        final String expectedResponseText = "error";
        final String cityName = "name";
        final String argument = "city remove " + cityName;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);

        City city = getSomeCities(request.getMessage().getUser()).get(0);
        when(cityService.get(cityName)).thenReturn(city);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse response = citySetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(cityService).remove(city);
    }

    @Test
    void setAddCityEmptyArgsTest() {
        final String expectedResponseText = "\n" +
                "${setter.city.commandwaitingstart}";
        final String argument = "city add";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();

        BotResponse response = citySetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).add(message.getChat(), message.getUser(), org.telegram.bot.commands.Set.class,  "${setter.command} ${setter.city.emptycommand} ${setter.city.add}");
    }

    @Test
    void setAddCityOnlyOneArgTest() {
        final String expectedErrorText = "error";
        final String argument = "city add name";
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows((BotException.class), () -> citySetter.set(request, argument));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(commandWaitingService).remove(message.getChat(), message.getUser());
    }

    @Test
    void setAddCityTest() {
        final String expectedResponseText = "\n" +
                "${setter.city.selectzonehelp}";
        final String ruName = "Тест";
        final String enName = "Test";
        final Long cityId = 1L;
        final String argument = "city add " + ruName + " " + enName;
        BotRequest request = TestUtils.getRequestFromGroup("set " + argument);
        Message message = request.getMessage();
        User user = message.getUser();

        doAnswer(answer -> ((City) answer.getArgument(0)).setId(cityId)).when(cityService).save(any(City.class));

        BotResponse response = citySetter.set(request, argument);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(commandWaitingService).remove(message.getChat(), user);
        ArgumentCaptor<City> cityArgumentCaptor = ArgumentCaptor.forClass(City.class);
        verify(cityService).save(cityArgumentCaptor.capture());

        City storedCity = cityArgumentCaptor.getValue();
        assertEquals(ruName, storedCity.getNameRu());
        assertEquals(enName, storedCity.getNameEn());
        assertEquals(TimeZone.getTimeZone(ZoneId.systemDefault()).getID(), storedCity.getTimeZone());
        assertEquals(user, storedCity.getUser());

        assertTimeZoneKeyboard(textResponse.getKeyboard(), cityId);
    }

    private void assertSelectKeyboard(Keyboard keyboard, List<City> cities) {
        assertKeyboardWithCities(keyboard, cities, false);
    }

    private void assertRemoveKeyboard(Keyboard keyboard, List<City> cities) {
        assertKeyboardWithCities(keyboard, cities, true);
    }

    private void assertKeyboardWithCities(Keyboard keyboard, List<City> cities, boolean deleteCommand) {
        assertNotNull(keyboard);

        int citiesSize = cities.size();
        List<List<KeyboardButton>> keyboardButtonsList = keyboard.getKeyboardButtonsList();
        assertEquals(8, keyboardButtonsList.size());

        for (int i = 0; i < citiesSize; i++) {
            List<KeyboardButton> row = keyboardButtonsList.get(i);
            assertEquals(1, row.size());

            City city = cities.get(i);
            KeyboardButton keyboardButton = row.get(0);

            String expectedCityName;
            if (deleteCommand) {
                expectedCityName = "❌" + city.getNameRu();
            } else {
                expectedCityName = city.getNameRu();
            }

            String expectedCallback;
            if (deleteCommand) {
                expectedCallback = "${setter.command} ${setter.city.emptycommand} ${setter.city.remove} " + (i + 1);
            } else {
                expectedCallback = "${setter.command} ${setter.city.emptycommand} ${setter.city.select} " + (i + 1);
            }

            assertEquals(expectedCityName, keyboardButton.getName());
            assertEquals(expectedCallback, keyboardButton.getCallback());
        }

        assertMainKeyboard(keyboardButtonsList.get(citiesSize), keyboardButtonsList.get(citiesSize + 1), keyboardButtonsList.get(citiesSize + 2), keyboardButtonsList.get(citiesSize + 3), keyboardButtonsList.get(citiesSize + 4));
    }

    private void assertMainKeyboard(Keyboard keyboard) {
        assertNotNull(keyboard);

        List<List<KeyboardButton>> keyboardButtonsList = keyboard.getKeyboardButtonsList();
        assertEquals(5, keyboardButtonsList.size());

        assertMainKeyboard(keyboardButtonsList.get(0), keyboardButtonsList.get(1), keyboardButtonsList.get(2), keyboardButtonsList.get(3), keyboardButtonsList.get(4));
    }

    private void assertMainKeyboard(List<KeyboardButton> selectRow, List<KeyboardButton> addRow, List<KeyboardButton> removeRow, List<KeyboardButton> updateRow, List<KeyboardButton> backRow) {
        assertEquals(1, selectRow.size());
        KeyboardButton selectButton = selectRow.get(0);
        assertEquals("⤴\uFE0F${setter.city.button.select}", selectButton.getName());
        assertEquals("${setter.command} ${setter.city.emptycommand} ${setter.city.select}", selectButton.getCallback());

        assertEquals(1, addRow.size());
        KeyboardButton addButton = addRow.get(0);
        assertEquals("\uD83C\uDD95${setter.city.button.add}", addButton.getName());
        assertEquals("${setter.command} ${setter.city.emptycommand} ${setter.city.add}", addButton.getCallback());

        assertEquals(1, removeRow.size());
        KeyboardButton removeButton = removeRow.get(0);
        assertEquals("❌${setter.city.button.remove}", removeButton.getName());
        assertEquals("${setter.command} ${setter.city.emptycommand} ${setter.city.remove}", removeButton.getCallback());

        assertEquals(1, updateRow.size());
        KeyboardButton updateButton = updateRow.get(0);
        assertEquals("\uD83D\uDD04${setter.city.button.update}", updateButton.getName());
        assertEquals("${setter.command} ${setter.city.emptycommand} ${setter.city.update}", updateButton.getCallback());

        assertEquals(1, backRow.size());
        KeyboardButton backButton = backRow.get(0);
        assertEquals("↩\uFE0F${setter.city.button.settings}", backButton.getName());
        assertEquals("${setter.command} back", backButton.getCallback());
    }

    private void assertTimeZoneKeyboard(Keyboard keyboard, long cityId) {
        assertNotNull(keyboard);

        DateUtils.TimeZones[] zones = DateUtils.TimeZones.values();
        List<List<KeyboardButton>> keyboardButtonsList = keyboard.getKeyboardButtonsList();
        assertEquals(zones.length, keyboardButtonsList.size());

        int i = 0;
        for (DateUtils.TimeZones zone : zones) {
            List<KeyboardButton> row = keyboardButtonsList.get(i);
            assertEquals(1, row.size());

            KeyboardButton button = row.get(0);
            assertEquals(zone.getZone(), button.getName());
            assertEquals("${setter.command} ${setter.city.emptycommand} ${setter.city.zone}" + " " + cityId + " " + zone.getZone(), button.getCallback());

            i = i + 1;
        }
    }

    private static List<City> getSomeCities(User user) {
        return List.of(
                new City().setId(1L).setNameRu("Тест1").setNameEn("Test1").setTimeZone("GMT+01:00").setUser(user),
                new City().setId(2L).setNameRu("Тест2").setNameEn("Test2").setTimeZone("GMT+02:00").setUser(user),
                new City().setId(3L).setNameRu("Тест3").setNameEn("Test3").setTimeZone("GMT+03:00").setUser(user)
        );
    }

}