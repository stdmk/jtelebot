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
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.EatenProduct;
import org.telegram.bot.domain.entities.calories.Product;
import org.telegram.bot.domain.entities.calories.UserCalories;
import org.telegram.bot.domain.entities.calories.UserCaloriesTarget;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.mapper.caloric.CaloricMapper;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.calories.EatenProductService;
import org.telegram.bot.services.calories.ProductService;
import org.telegram.bot.services.calories.UserCaloriesService;
import org.telegram.bot.services.calories.UserCaloriesTargetService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaloriesTest {

    private static final int MAX_SIZE_OF_SEARCH_RESULTS = 30;
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDate DATE = DATE_TIME.toLocalDate();

    @Mock
    private InternationalizationService internationalizationService;
    @Mock
    private SpeechService speechService;
    @Mock
    private ProductService productService;
    @Mock
    private EatenProductService eatenProductService;
    @Mock
    private UserCaloriesService userCaloriesService;
    @Mock
    private UserCaloriesTargetService userCaloriesTargetService;
    @Mock
    private UserCityService userCityService;
    @Mock
    private CaloricMapper caloricMapper;

    @InjectMocks
    private Calories calories;

    @Test
    void parseWithoutArgumentsEmptyDataTest() {
        final String expectedResponseText = """
                <b><u>${command.calories.caption}:</u></b>
                ${command.calories.eaten}: <b>0</b> ${command.calories.kcal}.\s
                <b><u>${command.calories.caption2}:</u></b>
                ${command.calories.proteins}: <b>0</b> ${command.calories.gramssymbol}.\s
                ${command.calories.fats}: <b>0</b> ${command.calories.gramssymbol}.\s
                ${command.calories.carbs}: <b>0</b> ${command.calories.gramssymbol}.\s
                
                """;
        BotRequest request = TestUtils.getRequestFromGroup("/calories");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        ZoneId zoneId = mock(ZoneId.class);
        when(userCityService.getZoneIdOfUserOrDefault(chat, user)).thenReturn(zoneId);
        UserCalories userCalories = new UserCalories()
                .setUser(user)
                .setDate(DATE)
                .setEatenProducts(new ArrayList<>());
        when(userCaloriesService.get(user, zoneId)).thenReturn(userCalories);
        org.telegram.bot.domain.Calories calories1 = new org.telegram.bot.domain.Calories(0, 0, 0, 0);
        when(caloricMapper.sum(anyCollection())).thenReturn(calories1);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithoutArgumentsAllDataWithoutTargetsTest() {
        final String expectedResponseText = """
                <b><u>${command.calories.caption}:</u></b>
                ${command.calories.eaten}: <b>1097</b> ${command.calories.kcal}.\s
                <b><u>${command.calories.caption2}:</u></b>
                ${command.calories.proteins}: <b>100</b> ${command.calories.gramssymbol}.\s
                ${command.calories.fats}: <b>33</b> ${command.calories.gramssymbol}.\s
                ${command.calories.carbs}: <b>100</b> ${command.calories.gramssymbol}.\s
                
                <u><b>23:30 — 23:50</b></u>: <b>560 ${command.calories.kcal}.</b>\s
                ${command.calories.proteins}: <b>70</b> ${command.calories.gramssymbol}.\s
                ${command.calories.fats}: <b>140</b> ${command.calories.gramssymbol}.\s
                ${command.calories.carbs}: <b>210</b> ${command.calories.gramssymbol}.\s
                -----------------------------
                <b>•</b> productName3 (150 ${command.calories.gramssymbol}.) — <b>360</b> ${command.calories.kcal}. <b>45</b> ${command.calories.proteinssymbol}. <b>90</b> ${command.calories.fatssymbol}. <b>135</b> ${command.calories.carbssymbol}.
                 /calories_del_3
                <b>•</b> productName2 (100 ${command.calories.gramssymbol}.) — <b>160</b> ${command.calories.kcal}. <b>20</b> ${command.calories.proteinssymbol}. <b>40</b> ${command.calories.fatssymbol}. <b>60</b> ${command.calories.carbssymbol}.
                 /calories_del_2
                <b>•</b> productName1 (50 ${command.calories.gramssymbol}.) — <b>40</b> ${command.calories.kcal}. <b>5</b> ${command.calories.proteinssymbol}. <b>10</b> ${command.calories.fatssymbol}. <b>15</b> ${command.calories.carbssymbol}.
                 /calories_del_1
                """;
        BotRequest request = TestUtils.getRequestFromGroup("/calories");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        ZoneId zoneId = mock(ZoneId.class);
        when(userCityService.getZoneIdOfUserOrDefault(chat, user)).thenReturn(zoneId);
        UserCalories userCalories = new UserCalories()
                .setUser(user)
                .setDate(DATE);
        userCalories.setEatenProducts(getSomeEatenProducts(userCalories));
        when(caloricMapper.toCalories(any(EatenProduct.class))).thenAnswer(answer -> getSomeCalories(answer.getArgument(0)));
        when(userCaloriesService.get(user, zoneId)).thenReturn(userCalories);
        org.telegram.bot.domain.Calories calories1 = new org.telegram.bot.domain.Calories(100, 33, 100, 1097);
        when(caloricMapper.sum(anyCollection())).thenReturn(calories1);
        UserCaloriesTarget userCaloriesTarget = new UserCaloriesTarget()
                .setId(1L)
                .setUser(user);
        when(userCaloriesTargetService.get(user)).thenReturn(userCaloriesTarget);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithoutArgumentsAllDataTest() {
        final String expectedResponseText = """
                <b><u>${command.calories.caption}:</u></b>
                ${command.calories.eaten}: <b>1097</b> ${command.calories.kcal}. (54,9%)
                ${command.calories.left}: <b>903</b> ${command.calories.kcal}
                
                <b><u>${command.calories.caption2}:</u></b>
                ${command.calories.proteins}: <b>100</b> ${command.calories.gramssymbol}. (100%)
                ${command.calories.fats}: <b>33</b> ${command.calories.gramssymbol}. (50%)
                ${command.calories.carbs}: <b>100</b> ${command.calories.gramssymbol}. (36,4%)
                
                <u><b>22:00 — 22:30</b></u>: <b>17840 ${command.calories.kcal}.</b> (892%)\s
                ${command.calories.proteins}: <b>2230</b> ${command.calories.gramssymbol}. (2230%)\s
                ${command.calories.fats}: <b>4460</b> ${command.calories.gramssymbol}. (6757,6%)\s
                ${command.calories.carbs}: <b>6690</b> ${command.calories.gramssymbol}. (2432,7%)\s
                -----------------------------
                <b>•</b> productName12 (600 ${command.calories.gramssymbol}.) — <b>5760</b> ${command.calories.kcal}. <b>720</b> ${command.calories.proteinssymbol}. <b>1440</b> ${command.calories.fatssymbol}. <b>2160</b> ${command.calories.carbssymbol}.
                 /calories_del_12
                <b>•</b> productName11 (550 ${command.calories.gramssymbol}.) — <b>4840</b> ${command.calories.kcal}. <b>605</b> ${command.calories.proteinssymbol}. <b>1210</b> ${command.calories.fatssymbol}. <b>1815</b> ${command.calories.carbssymbol}.
                 /calories_del_11
                <b>•</b> productName10 (500 ${command.calories.gramssymbol}.) — <b>4000</b> ${command.calories.kcal}. <b>500</b> ${command.calories.proteinssymbol}. <b>1000</b> ${command.calories.fatssymbol}. <b>1500</b> ${command.calories.carbssymbol}.
                 /calories_del_10
                <b>•</b> productName9 (450 ${command.calories.gramssymbol}.) — <b>3240</b> ${command.calories.kcal}. <b>405</b> ${command.calories.proteinssymbol}. <b>810</b> ${command.calories.fatssymbol}. <b>1215</b> ${command.calories.carbssymbol}.
                 /calories_del_9
                
                <u><b>22:40 — 23:10</b></u>: <b>6960 ${command.calories.kcal}.</b> (348%)\s
                ${command.calories.proteins}: <b>870</b> ${command.calories.gramssymbol}. (870%)\s
                ${command.calories.fats}: <b>1740</b> ${command.calories.gramssymbol}. (2636,4%)\s
                ${command.calories.carbs}: <b>2610</b> ${command.calories.gramssymbol}. (949,1%)\s
                -----------------------------
                <b>•</b> productName8 (400 ${command.calories.gramssymbol}.) — <b>2560</b> ${command.calories.kcal}. <b>320</b> ${command.calories.proteinssymbol}. <b>640</b> ${command.calories.fatssymbol}. <b>960</b> ${command.calories.carbssymbol}.
                 /calories_del_8
                <b>•</b> productName7 (350 ${command.calories.gramssymbol}.) — <b>1960</b> ${command.calories.kcal}. <b>245</b> ${command.calories.proteinssymbol}. <b>490</b> ${command.calories.fatssymbol}. <b>735</b> ${command.calories.carbssymbol}.
                 /calories_del_7
                <b>•</b> productName6 (300 ${command.calories.gramssymbol}.) — <b>1440</b> ${command.calories.kcal}. <b>180</b> ${command.calories.proteinssymbol}. <b>360</b> ${command.calories.fatssymbol}. <b>540</b> ${command.calories.carbssymbol}.
                 /calories_del_6
                <b>•</b> productName5 (250 ${command.calories.gramssymbol}.) — <b>1000</b> ${command.calories.kcal}. <b>125</b> ${command.calories.proteinssymbol}. <b>250</b> ${command.calories.fatssymbol}. <b>375</b> ${command.calories.carbssymbol}.
                 /calories_del_5
                
                <u><b>23:20 — 23:50</b></u>: <b>1200 ${command.calories.kcal}.</b> (60%)\s
                ${command.calories.proteins}: <b>150</b> ${command.calories.gramssymbol}. (150%)\s
                ${command.calories.fats}: <b>300</b> ${command.calories.gramssymbol}. (454,5%)\s
                ${command.calories.carbs}: <b>450</b> ${command.calories.gramssymbol}. (163,6%)\s
                -----------------------------
                <b>•</b> productName4 (200 ${command.calories.gramssymbol}.) — <b>640</b> ${command.calories.kcal}. <b>80</b> ${command.calories.proteinssymbol}. <b>160</b> ${command.calories.fatssymbol}. <b>240</b> ${command.calories.carbssymbol}.
                 /calories_del_4
                <b>•</b> productName3 (150 ${command.calories.gramssymbol}.) — <b>360</b> ${command.calories.kcal}. <b>45</b> ${command.calories.proteinssymbol}. <b>90</b> ${command.calories.fatssymbol}. <b>135</b> ${command.calories.carbssymbol}.
                 /calories_del_3
                <b>•</b> productName2 (100 ${command.calories.gramssymbol}.) — <b>160</b> ${command.calories.kcal}. <b>20</b> ${command.calories.proteinssymbol}. <b>40</b> ${command.calories.fatssymbol}. <b>60</b> ${command.calories.carbssymbol}.
                 /calories_del_2
                <b>•</b> productName1 (50 ${command.calories.gramssymbol}.) — <b>40</b> ${command.calories.kcal}. <b>5</b> ${command.calories.proteinssymbol}. <b>10</b> ${command.calories.fatssymbol}. <b>15</b> ${command.calories.carbssymbol}.
                 /calories_del_1
                """;
        BotRequest request = TestUtils.getRequestFromGroup("/calories");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        ZoneId zoneId = mock(ZoneId.class);
        when(userCityService.getZoneIdOfUserOrDefault(chat, user)).thenReturn(zoneId);
        UserCalories userCalories = new UserCalories()
                .setUser(user)
                .setDate(DATE);
        userCalories.setEatenProducts(LongStream.range(1, 13).mapToObj(id -> getSomeEatenProduct(userCalories, id)).toList());
        when(caloricMapper.toCalories(any(EatenProduct.class))).thenAnswer(answer -> getSomeCalories(answer.getArgument(0)));
        when(userCaloriesService.get(user, zoneId)).thenReturn(userCalories);
        org.telegram.bot.domain.Calories calories1 = new org.telegram.bot.domain.Calories(100, 33, 100, 1097);
        when(caloricMapper.sum(anyCollection())).thenReturn(calories1);
        UserCaloriesTarget userCaloriesTarget = new UserCaloriesTarget()
                .setId(1L)
                .setUser(user)
                .setProteins(100D)
                .setFats(66D)
                .setCarbs(275D)
                .setCalories(2000D);
        when(userCaloriesTargetService.get(user)).thenReturn(userCaloriesTarget);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithoutArgumentsAllDataNoneLeftCaloriesTest() {
        final String expectedResponseText = """
                <b><u>${command.calories.caption}:</u></b>
                ${command.calories.eaten}: <b>2100</b> ${command.calories.kcal}. (105%)
                ${command.calories.noneleft}: <b>100</b> ${command.calories.kcal}
                
                <b><u>${command.calories.caption2}:</u></b>
                ${command.calories.proteins}: <b>100</b> ${command.calories.gramssymbol}. (100%)
                ${command.calories.fats}: <b>33</b> ${command.calories.gramssymbol}. (50%)
                ${command.calories.carbs}: <b>100</b> ${command.calories.gramssymbol}. (36,4%)
                
                <u><b>23:50</b></u>: <b>560 ${command.calories.kcal}.</b> (28%)\s
                ${command.calories.proteins}: <b>70</b> ${command.calories.gramssymbol}. (70%)\s
                ${command.calories.fats}: <b>140</b> ${command.calories.gramssymbol}. (212,1%)\s
                ${command.calories.carbs}: <b>210</b> ${command.calories.gramssymbol}. (76,4%)\s
                -----------------------------
                <b>•</b> productName1 (50 ${command.calories.gramssymbol}.) — <b>40</b> ${command.calories.kcal}. <b>5</b> ${command.calories.proteinssymbol}. <b>10</b> ${command.calories.fatssymbol}. <b>15</b> ${command.calories.carbssymbol}.
                 /calories_del_1
                <b>•</b> productName2 (100 ${command.calories.gramssymbol}.) — <b>160</b> ${command.calories.kcal}. <b>20</b> ${command.calories.proteinssymbol}. <b>40</b> ${command.calories.fatssymbol}. <b>60</b> ${command.calories.carbssymbol}.
                 /calories_del_2
                <b>•</b> productName3 (150 ${command.calories.gramssymbol}.) — <b>360</b> ${command.calories.kcal}. <b>45</b> ${command.calories.proteinssymbol}. <b>90</b> ${command.calories.fatssymbol}. <b>135</b> ${command.calories.carbssymbol}.
                 /calories_del_3
                """;
        BotRequest request = TestUtils.getRequestFromGroup("/calories");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        ZoneId zoneId = mock(ZoneId.class);
        when(userCityService.getZoneIdOfUserOrDefault(chat, user)).thenReturn(zoneId);
        UserCalories userCalories = new UserCalories()
                .setUser(user)
                .setDate(DATE);
        List<EatenProduct> someEatenProducts = getSomeEatenProducts(userCalories);
        someEatenProducts.forEach(eatenProduct -> eatenProduct.setDateTime(someEatenProducts.get(0).getDateTime()));
        userCalories.setEatenProducts(someEatenProducts);
        when(caloricMapper.toCalories(any(EatenProduct.class))).thenAnswer(answer -> getSomeCalories(answer.getArgument(0)));
        when(userCaloriesService.get(user, zoneId)).thenReturn(userCalories);
        org.telegram.bot.domain.Calories calories1 = new org.telegram.bot.domain.Calories(100, 33, 100, 2100);
        when(caloricMapper.sum(anyCollection())).thenReturn(calories1);
        UserCaloriesTarget userCaloriesTarget = new UserCaloriesTarget()
                .setId(1L)
                .setUser(user)
                .setProteins(100D)
                .setFats(66D)
                .setCarbs(275D)
                .setCalories(2000D);
        when(userCaloriesTargetService.get(user)).thenReturn(userCaloriesTarget);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithUnknownCommandAsArgumentTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_abv");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithAddProductCommandAsArgumentCurreptedIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_add_product_a");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithAddProductCommandAsArgumentNotExistenceProductIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_add_product_1");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithAddProductCommandAsArgumentAddNewProductTest() {
        final String expectedResponseText = "${command.calories.saveproduct}:\n" +
                "productName1 <b>80</b> ${command.calories.kcal} (<b>10</b> ${command.calories.proteinssymbol}. <b>20</b> ${command.calories.fatssymbol}. <b>30</b> ${command.calories.carbssymbol}.)";
        final long productId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("/calories_add_product_" + productId);

        Product product = getSomeProduct(productId, request.getMessage().getUser());
        when(productService.get(productId)).thenReturn(product);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<Product> productArgumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productService).save(productArgumentCaptor.capture());

        Product savedProduct = productArgumentCaptor.getValue();
        assertNull(savedProduct.getId());
        assertEquals(product.getName(), savedProduct.getName());
        assertEquals(product.getUser(), savedProduct.getUser());
        assertEquals(product.getProteins(), savedProduct.getProteins());
        assertEquals(product.getFats(), savedProduct.getFats());
        assertEquals(product.getCarbs(), savedProduct.getCarbs());
        assertEquals(product.getCaloric(), savedProduct.getCaloric());
    }

    @Test
    void parseWithAddProductCommandAsArgumentUpdateProductTest() {
        final String expectedResponseText = "${command.calories.updateproduct}:\n" +
                "productName2 <b>80</b> ${command.calories.kcal} (<b>10</b> ${command.calories.proteinssymbol}. <b>20</b> ${command.calories.fatssymbol}. <b>30</b> ${command.calories.carbssymbol}.)";
        final long productId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("/calories_add_product_" + productId);
        User user = request.getMessage().getUser();

        Product product = getSomeProduct(productId, user);
        when(productService.get(productId)).thenReturn(product);
        Product updatingProduct = getSomeProduct(2L, user);
        when(productService.get(user, product.getName())).thenReturn(updatingProduct);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<Product> productArgumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productService).save(productArgumentCaptor.capture());

        Product savedProduct = productArgumentCaptor.getValue();
        assertEquals(updatingProduct.getId(), savedProduct.getId());
        assertEquals(updatingProduct.getName(), savedProduct.getName());
        assertEquals(updatingProduct.getUser(), savedProduct.getUser());
        assertEquals(product.getProteins(), savedProduct.getProteins());
        assertEquals(product.getFats(), savedProduct.getFats());
        assertEquals(product.getCarbs(), savedProduct.getCarbs());
        assertEquals(product.getCaloric(), savedProduct.getCaloric());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abv", "a_a", "1_a"})
    void parseWithAddCaloriesByProductIdCommandAsArgumentCurreptedCommandTest(String argument) {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_add_" + argument);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithAddCaloriesByProductIdCommandAsArgumentNotExistenceProductIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_add_1_50");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithAddCaloriesByProductIdCommandAsArgumentTest() {
        final String expectedResponseText = """
                ${command.calories.added}: <b>300</b> ${command.calories.kcal}.
                (<b>10</b> ${command.calories.proteinssymbol}. <b>3</b> ${command.calories.fatssymbol}. <b>20</b> ${command.calories.carbssymbol}. )
                
                <b><u>${command.calories.caption}:</u></b>
                ${command.calories.eaten}: <b>0</b> ${command.calories.kcal}.\s
                <b><u>${command.calories.caption2}:</u></b>
                ${command.calories.proteins}: <b>0</b> ${command.calories.gramssymbol}.\s
                ${command.calories.fats}: <b>0</b> ${command.calories.gramssymbol}.\s
                ${command.calories.carbs}: <b>0</b> ${command.calories.gramssymbol}.\s
                
                """;
        final long productId = 1L;
        final int grams = 50;
        BotRequest request = TestUtils.getRequestFromGroup("/calories_add_" + productId + "_" + grams);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        Product product = getSomeProduct(productId, user);
        when(productService.get(productId)).thenReturn(product);
        ZoneId zoneId = mock(ZoneId.class);
        when(userCityService.getZoneIdOfUserOrDefault(chat, user)).thenReturn(zoneId);
        UserCalories userCalories = new UserCalories()
                .setUser(user)
                .setDate(DATE)
                .setEatenProducts(List.of());
        when(userCaloriesService.addCalories(user, zoneId, product, grams)).thenReturn(userCalories);
        org.telegram.bot.domain.Calories caloriesOfAddedProduct = new org.telegram.bot.domain.Calories(10, 3, 20, 300);
        when(caloricMapper.toCalories(product, grams)).thenReturn(caloriesOfAddedProduct);
        org.telegram.bot.domain.Calories caloriesTotal = new org.telegram.bot.domain.Calories(0, 0, 0, 0);
        when(caloricMapper.sum(anyCollection())).thenReturn(caloriesTotal);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithDeleteProductAsArgumentCorruptedIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_del_product_a");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithDeleteProductAsArgumentNotExistenceProductIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_del_product_1");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithDeleteProductAsArgumentSomeOneElseEatenProductTest() {
        final String expectedErrorText = "error";
        final long productId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("/calories_del_product_" + productId);

        when(productService.get(productId))
                .thenReturn(getSomeProduct(productId, new User().setUserId(TestUtils.ANOTHER_USER_ID)));
        when(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithDeleteProductAsArgumentTest() {
        final String expectedResponseText = "saved";
        final long productId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("/calories_del_product_" + productId);

        Product product = getSomeProduct(productId, request.getMessage().getUser());
        when(productService.get(productId)).thenReturn(product);
        when(speechService.getRandomMessageByTag(BotSpeechTag.SAVED)).thenReturn(expectedResponseText);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(productService).remove(product);
    }

    @Test
    void parseWithDeleteEatenProductAsArgumentCurreptedIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_del_a");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithDeleteEatenProductAsArgumentNotExistenceProductIdTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories_del_1");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithDeleteEatenProductAsArgumentSomeOneElseEatenProductTest() {
        final String expectedErrorText = "error";
        final long eatenProductId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("/calories_del_" + eatenProductId);

        when(eatenProductService.get(eatenProductId))
                .thenReturn(getSomeEatenProduct(new UserCalories().setUser(new User().setUserId(TestUtils.ANOTHER_USER_ID)), eatenProductId));
        when(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithDeleteEatenProductAsArgumentTest() {
        final String expectedResponseText = """
                <b><u>${command.calories.caption}:</u></b>
                ${command.calories.eaten}: <b>0</b> ${command.calories.kcal}.\s
                <b><u>${command.calories.caption2}:</u></b>
                ${command.calories.proteins}: <b>0</b> ${command.calories.gramssymbol}.\s
                ${command.calories.fats}: <b>0</b> ${command.calories.gramssymbol}.\s
                ${command.calories.carbs}: <b>0</b> ${command.calories.gramssymbol}.\s
                
                """;
        final long eatenProductId = 1L;
        BotRequest request = TestUtils.getRequestFromGroup("/calories_del_" + eatenProductId);
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        EatenProduct eatenProduct = getSomeEatenProduct(new UserCalories().setUser(user), eatenProductId);
        when(eatenProductService.get(eatenProductId)).thenReturn(eatenProduct);
        ZoneId zoneId = mock(ZoneId.class);
        when(userCityService.getZoneIdOfUserOrDefault(chat, user)).thenReturn(zoneId);
        UserCalories userCalories = new UserCalories()
                .setUser(user)
                .setDate(DATE)
                .setEatenProducts(List.of());
        when(userCaloriesService.get(user, zoneId)).thenReturn(userCalories);
        org.telegram.bot.domain.Calories calories1 = new org.telegram.bot.domain.Calories(0, 0, 0, 0);
        when(caloricMapper.sum(anyCollection())).thenReturn(calories1);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(eatenProductService).remove(eatenProduct);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0g", "1g", "1g "})
    void parseWithAddingCaloriesByProductCorruptedCommandTest(String command) {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("/calories " + command);

        setUpInternationalization();
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithAddingCaloriesByProductNotFoundCommandTest() {
        final String expectedResponseText = """
                ${command.calories.unknownproduct}: <b>name</b>
                
                productName1 <b>80</b> ${command.calories.kcal} (<b>10</b> ${command.calories.proteinssymbol}. <b>20</b> ${command.calories.fatssymbol}. <b>30</b> ${command.calories.carbssymbol}.) /calories_add_1_50 <b>+40</b> ${command.calories.kcal}
                productName2 <b>160</b> ${command.calories.kcal} (<b>20</b> ${command.calories.proteinssymbol}. <b>40</b> ${command.calories.fatssymbol}. <b>60</b> ${command.calories.carbssymbol}.) /calories_add_2_50 <b>+80</b> ${command.calories.kcal}
                productName3 <b>240</b> ${command.calories.kcal} (<b>30</b> ${command.calories.proteinssymbol}. <b>60</b> ${command.calories.fatssymbol}. <b>90</b> ${command.calories.carbssymbol}.) /calories_add_3_50 <b>+120</b> ${command.calories.kcal}""";
        final String productName = "name";
        final int grams = 50;
        BotRequest request = TestUtils.getRequestFromGroup("/calories " + productName + " " + grams + "g");
        User user = request.getMessage().getUser();

        setUpInternationalization();
        when(productService.find(user, productName, MAX_SIZE_OF_SEARCH_RESULTS)).thenReturn(getSomeProducts(user));

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithAddingCaloriesByProductCommandTest() {
        final String expectedResponseText = """
                ${command.calories.added}: <b>300</b> ${command.calories.kcal}.
                (<b>10</b> ${command.calories.proteinssymbol}. <b>3</b> ${command.calories.fatssymbol}. <b>20</b> ${command.calories.carbssymbol}. )
                
                <b><u>${command.calories.caption}:</u></b>
                ${command.calories.eaten}: <b>0</b> ${command.calories.kcal}.\s
                <b><u>${command.calories.caption2}:</u></b>
                ${command.calories.proteins}: <b>0</b> ${command.calories.gramssymbol}.\s
                ${command.calories.fats}: <b>0</b> ${command.calories.gramssymbol}.\s
                ${command.calories.carbs}: <b>0</b> ${command.calories.gramssymbol}.\s
                
                """;
        final String productName = "name";
        final int grams = 50;
        BotRequest request = TestUtils.getRequestFromGroup("/calories " + productName + " " + grams + "g");
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        setUpInternationalization();
        Product product = getSomeProduct(1L, user);
        when(productService.get(user, productName)).thenReturn(product);
        ZoneId zoneId = mock(ZoneId.class);
        when(userCityService.getZoneIdOfUserOrDefault(chat, user)).thenReturn(zoneId);
        UserCalories userCalories = new UserCalories()
                .setUser(user)
                .setDate(DATE)
                .setEatenProducts(List.of());
        when(userCaloriesService.addCalories(user, zoneId, product, grams)).thenReturn(userCalories);
        org.telegram.bot.domain.Calories caloriesOfAddedProduct = new org.telegram.bot.domain.Calories(10, 3, 20, 300);
        when(caloricMapper.toCalories(product, grams)).thenReturn(caloriesOfAddedProduct);
        org.telegram.bot.domain.Calories caloriesTotal = new org.telegram.bot.domain.Calories(0, 0, 0, 0);
        when(caloricMapper.sum(anyCollection())).thenReturn(caloriesTotal);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    @Test
    void parseWithAddingProductCommandTooLongNameTest() {
        final String expectedErrorText = "error";
        final String productName = "name".repeat(64);
        BotRequest request = TestUtils.getRequestFromGroup("/calories " + productName + " 200k");

        setUpInternationalization();
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithAddingProductCommandWithoutKcalValueTest() {
        final double expectedProteins = 10D;
        final double expectedFats = 20D;
        final double expectedCarbs = 30D;
        final double expectedCaloric = 300D;
        final String expectedResponseText = "${command.calories.saveproduct}:\n" +
                "name <b>300</b> ${command.calories.kcal} (<b>10</b> ${command.calories.proteinssymbol}. <b>20</b> ${command.calories.fatssymbol}. <b>30</b> ${command.calories.carbssymbol}.)";
        final String productName = "name";
        BotRequest request = TestUtils.getRequestFromGroup("/calories " + productName + " 10p 20f 30c");

        setUpInternationalization();
        when(caloricMapper.toCaloric(any(Double.class), any(Double.class), any(Double.class))).thenReturn(expectedCaloric);

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        ArgumentCaptor<Product> productArgumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productService).save(productArgumentCaptor.capture());

        Product savedProduct = productArgumentCaptor.getValue();
        assertNull(savedProduct.getId());
        assertEquals(productName, savedProduct.getName());
        assertEquals(request.getMessage().getUser(), savedProduct.getUser());
        assertEquals(expectedProteins, savedProduct.getProteins());
        assertEquals(expectedFats, savedProduct.getFats());
        assertEquals(expectedCarbs, savedProduct.getCarbs());
        assertEquals(expectedCaloric, savedProduct.getCaloric());
    }

    @Test
    void parseWithGetProductInfoCommandWhenNothingFoundTest() {
        final String expectedErrorText = "error";
        final String productName = "name";
        BotRequest request = TestUtils.getRequestFromGroup("/calories " + productName);

        setUpInternationalization();
        when(productService.find(productName, MAX_SIZE_OF_SEARCH_RESULTS)).thenReturn(new PageImpl<>(List.of()));
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);

        BotException botException = assertThrows(BotException.class, () -> calories.parse(request));

        assertEquals(expectedErrorText, botException.getMessage());
    }

    @Test
    void parseWithGetProductInfoCommandTest() {
        final String expectedResponseText = """
                productName1 <b>80</b> ${command.calories.kcal} (<b>10</b> ${command.calories.proteinssymbol}. <b>20</b> ${command.calories.fatssymbol}. <b>30</b> ${command.calories.carbssymbol}.) /calories_del_product_1
                productName2 <b>160</b> ${command.calories.kcal} (<b>20</b> ${command.calories.proteinssymbol}. <b>40</b> ${command.calories.fatssymbol}. <b>60</b> ${command.calories.carbssymbol}.) /calories_add_product_2
                productName3 <b>240</b> ${command.calories.kcal} (<b>30</b> ${command.calories.proteinssymbol}. <b>60</b> ${command.calories.fatssymbol}. <b>90</b> ${command.calories.carbssymbol}.) /calories_del_product_3
                ${command.calories.totalproductsfound}: <b>3</b>""";
        final String productName = "name";
        BotRequest request = TestUtils.getRequestFromGroup("/calories " + productName);
        List<Product> products = getSomeProducts(request.getMessage().getUser());
        Product product = products.get(1);
        product.setUser(new User().setUserId(TestUtils.ANOTHER_USER_ID));

        setUpInternationalization();

        when(productService.find(productName, MAX_SIZE_OF_SEARCH_RESULTS)).thenReturn(new PageImpl<>(products));

        BotResponse response = calories.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());
    }

    private void setUpInternationalization() {
        when(internationalizationService.getAllTranslations("command.calories.proteinssymbol")).thenReturn(Set.of("p"));
        when(internationalizationService.getAllTranslations("command.calories.fatssymbol")).thenReturn(Set.of("f"));
        when(internationalizationService.getAllTranslations("command.calories.carbssymbol")).thenReturn(Set.of("c"));
        when(internationalizationService.getAllTranslations("command.calories.calsymbol")).thenReturn(Set.of("k"));
        when(internationalizationService.getAllTranslations("command.calories.gramssymbol")).thenReturn(Set.of("g"));
        ReflectionTestUtils.invokeMethod(calories, "postConstruct");
    }

    private List<EatenProduct> getSomeEatenProducts(UserCalories userCalories) {
        return List.of(
                getSomeEatenProduct(userCalories, 1L),
                getSomeEatenProduct(userCalories, 2L),
                getSomeEatenProduct(userCalories, 3L));
    }

    private EatenProduct getSomeEatenProduct(UserCalories userCalories, Long id) {
        return new EatenProduct()
                .setId(id)
                .setProduct(getSomeProduct(id, userCalories.getUser()))
                .setGrams(id * 50D)
                .setDateTime(DATE_TIME.minusMinutes(id * 10))
                .setUserCalories(userCalories);
    }

    private List<Product> getSomeProducts(User user) {
        return LongStream.range(1, 4).mapToObj(id -> getSomeProduct(id, user)).toList();
    }

    private Product getSomeProduct(Long id, User user) {
        return new Product()
                .setId(id)
                .setUser(user)
                .setName("productName" + id)
                .setProteins(id * 10)
                .setFats(id * 20)
                .setCarbs(id * 30)
                .setCaloric(id * 80);
    }

    private org.telegram.bot.domain.Calories getSomeCalories(EatenProduct eatenProduct) {
        Product product = eatenProduct.getProduct();
        Double grams = eatenProduct.getGrams();

        double proteins = product.getProteins() / 100 * grams;
        double fats = product.getFats() / 100 * grams;
        double carbs = product.getCarbs() / 100 * grams;
        double caloric = product.getCaloric() / 100 * grams;

        return new org.telegram.bot.domain.Calories(proteins, fats, carbs, caloric);
    }

}