package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrieveTest {

    private static final String URL = "http://example.com";
    private static final String PATTERN = "\"temp\":(-?\\d+)";
    private static final String DATA = "<!DOCTYPE html><html class=\"i-ua_js_no i-ua_css_standard\" lang=\"ru\"><head><meta charset=\"utf-8\"/><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/><title>Почасовой прогноз погоды для Твери</title><script>;(function(d,e,c,r){e=d.documentElement;c=\"className\";r=\"replace\";e[c]=e[c][r](\"i-ua_js_no\",\"i-ua_js_yes\");if(d.compatMode!=\"CSS1Compat\")e[c]=e[c][r](\"i-ua_css_standart\",\"i-ua_css_quirks\")})(document);;(function(d,e,c,n,w,v,f){e=d.documentElement;c=\"className\";n=\"createElementNS\";f=\"firstChild\";w=\"http://www.w3.org/2000/svg\";e[c]+=\" i-ua_svg_\"+(!!d[n]&&!!d[n](w,\"svg\").createSVGRect?\"yes\":\"no\");v=d.createElement(\"div\");v.innerHTML=\"<svg/>\";e[c]+=\" i-ua_inlinesvg_\"+((v[f]&&v[f].namespaceURI)==w?\"yes\":\"no\");})(document);</script><meta name=\"viewport\" content=\"width=device-width, minimum-scale=1.0\"/><link rel=\"stylesheet\" href=\"//yastatic.net/s3/weather-frontend/302/mini.bundles/index/_index.css\"/><link rel=\"apple-touch-icon-precomposed\" href=\"/static/apple-touch-icon-57-precomposed.png\" sizes=\"57x57\"/><link rel=\"apple-touch-icon-precomposed\" href=\"/static/apple-touch-icon-72-precomposed.png\" sizes=\"72x72\"/><link rel=\"apple-touch-icon-precomposed\" href=\"/static/apple-touch-icon-114-precomposed.png\" sizes=\"114x114\"/><link rel=\"apple-touch-icon-precomposed\" href=\"/static/apple-touch-icon-120-precomposed.png\" sizes=\"120x120\"/><link rel=\"apple-touch-icon-precomposed\" href=\"/static/apple-touch-icon-144-precomposed.png\" sizes=\"144x144\"/><link rel=\"apple-touch-icon-precomposed\" href=\"/static/apple-touch-icon-152-precomposed.png\" sizes=\"152x152\"/><meta name=\"mobile-web-app-capable\" content=\"yes\"/><meta name=\"apple-mobile-web-app-capable\" content=\"yes\"/><meta name=\"apple-mobile-web-app-title\" content=\"Погода\"/><meta name=\"description\" content=\"Почасовой прогноз погоды для Твери на сегодня. Температура воздуха, осадки, облачность, скорость ветра.\"/><link rel=\"manifest\" href=\"/manifest.ru.json\"/><meta property=\"og:title\" content=\"Почасовой прогноз погоды для Твери\"/><meta property=\"og:description\" content=\"Почасовой прогноз погоды для Твери на сегодня. Температура воздуха, осадки, облачность, скорость ветра.\"/><meta property=\"og:image\"/><meta property=\"og:type\" content=\"website\"/><meta property=\"og:site_name\" content=\"Яндекс.Погода\"/><link rel=\"shortcut icon\" href=\"//yandex.ru/pogoda/static/favicon.ico?v=3\"/><meta name=\"msapplication-TileImage\" content=\"/tile-square-transp.png\"/><meta name=\"msapplication-TileColor\" content=\"#ee0000\"/><meta name=\"application-name\" content=\"Яндекс.Погода\"/><meta name=\"msapplication-square70x70logo\" content=\"/tile-tiny-transp.png\"/><meta name=\"msapplication-square150x150logo\" content=\"/tile-square-transp.png\"/><meta name=\"msapplication-wide310x150logo\" content=\"/tile-wide-transp.png\"/><meta name=\"msapplication-square310x310logo\" content=\"/tile-large-transp.png\"/><link rel=\"canonical\" href=\"/undefined\"/></head><body class=\"b-page b-page_theme_white container b-page__body i-ua i-global i-bem\" data-bem='{\"b-page\":{},\"i-ua\":{},\"i-global\":{\"lang\":\"ru\",\"tld\":\"ru\",\"content-region\":\"ru\",\"click-host\":\"//clck.yandex.ru\",\"passport-host\":\"https://passport.yandex.ru\",\"pass-host\":\"https://pass.yandex.ru\",\"social-host\":\"https://social.yandex.ru\",\"export-host\":\"https://export.yandex.ru\",\"login\":\"\",\"id\":\"weather-mini\"}}'><div class=\"container__row container__row_main\"><div class=\"fact\"><div class=\"temperature-wrapper\"><span class=\"preposition\">в</span> <span class=\"city city_type_selectable i-bem\" tabindex=\"1\" data-bem='{\"city\":{}}'>Твери</span><div class=\"portal-suggest portal-suggest_hidden_yes i-bem\" data-bem='{\"portal-suggest\":{}}'><span class=\"input input_suggest_yes input_size_m input_theme_normal i-bem\" data-bem='{\"input\":{\"dataprovider\":{\"url\":\"//suggest-maps.yandex.ru/suggest-geo\",\"name\":\"suggest-provider\",\"params\":{\"v\":8,\"lang\":\"ru_RU\",\"search_type\":\"weather\",\"n\":10,\"ll\":\"35.911851,56.859561\",\"spn\":\"0.5,0.5\",\"client_id\":\"weather_v1\"}},\"live\":false}}'><span class=\"input__box\"><span class=\"input__clear\" unselectable=\"on\"> </span><input class=\"input__control\" id=\"portal-suggest\" name=\"portal-suggest\" aria-labelledby=\"labelportal-suggest hintportal-suggest\" tabindex=\"1\" autocomplete=\"off\" maxlength=\"400\"/></span></span></div> <span class=\"temp-current i-bem\" data-bem='{\"temp-current\":{\"temp\":24}}'>+24</span> ˚<span class=\"temp-system i-bem\" tabindex=\"2\" data-bem='{\"temp-system\":{}}'>C</span></div><div class=\"today-forecast\">Сейчас облачно с прояснениями, ветер 1 м/с</div><div class=\"sparkline\"><div class=\"sparkline__hour\">11 ч</div><hr class=\"sparkline__spark sparkline__spark_diff_3 t t_c_25\"/><hr class=\"sparkline__spark sparkline__spark_diff_2 t t_c_26\"/><hr class=\"sparkline__spark sparkline__spark_diff_1 t t_c_27\"/><hr class=\"sparkline__spark sparkline__spark_diff_1 t t_c_27\"/><hr class=\"sparkline__spark sparkline__spark_diff_0 t t_c_28\"/><hr class=\"sparkline__spark sparkline__spark_diff_0 t t_c_28\"/><hr class=\"sparkline__spark sparkline__spark_diff_1 t t_c_27\"/><hr class=\"sparkline__spark sparkline__spark_diff_2 t t_c_26\"/><hr class=\"sparkline__spark sparkline__spark_diff_3 t t_c_25\"/><hr class=\"sparkline__spark sparkline__spark_diff_5 t t_c_23\"/><hr class=\"sparkline__spark sparkline__spark_diff_7 t t_c_21\"/><hr class=\"sparkline__spark sparkline__spark_diff_9 t t_c_19\"/><hr class=\"sparkline__spark sparkline__spark_diff_9 t t_c_19\"/><hr class=\"sparkline__spark sparkline__spark_diff_10 t t_c_18\"/><div class=\"sparkline__hour\">0 ч</div></div><div class=\"temp-chart temp-chart_size_m i-bem\" data-bem='{\"temp-chart\":{}}'><div class=\"temp-chart__inner\"><p class=\"temp-chart__legend\">по часам</p><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722067200\">11 ч</p><div class=\"temp-chart__item temp-chart__item_diff_3\"><div class=\"temp-chart__temp\" data-t=\"25\">+25</div><hr class=\"temp-chart__hr t t_c_25\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722070800\">12</p><div class=\"temp-chart__item temp-chart__item_diff_2\"><div class=\"temp-chart__temp\" data-t=\"26\">+26</div><hr class=\"temp-chart__hr t t_c_26\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722074400\">13</p><div class=\"temp-chart__item temp-chart__item_diff_1\"><div class=\"temp-chart__temp\" data-t=\"27\">+27</div><hr class=\"temp-chart__hr t t_c_27\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722078000\">14</p><div class=\"temp-chart__item temp-chart__item_diff_1\"><div class=\"temp-chart__temp\" data-t=\"27\">+27</div><hr class=\"temp-chart__hr t t_c_27\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722081600\">15</p><div class=\"temp-chart__item temp-chart__item_diff_0\"><div class=\"temp-chart__temp\" data-t=\"28\">+28</div><hr class=\"temp-chart__hr t t_c_28\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722085200\">16</p><div class=\"temp-chart__item temp-chart__item_diff_0\"><div class=\"temp-chart__temp\" data-t=\"28\">+28</div><hr class=\"temp-chart__hr t t_c_28\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722088800\">17</p><div class=\"temp-chart__item temp-chart__item_diff_1\"><div class=\"temp-chart__temp\" data-t=\"27\">+27</div><hr class=\"temp-chart__hr t t_c_27\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722092400\">18</p><div class=\"temp-chart__item temp-chart__item_diff_2\"><div class=\"temp-chart__temp\" data-t=\"26\">+26</div><hr class=\"temp-chart__hr t t_c_26\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722096000\">19</p><div class=\"temp-chart__item temp-chart__item_diff_3\"><div class=\"temp-chart__temp\" data-t=\"25\">+25</div><hr class=\"temp-chart__hr t t_c_25\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722099600\">20</p><div class=\"temp-chart__item temp-chart__item_diff_5\"><div class=\"temp-chart__temp\" data-t=\"23\">+23</div><hr class=\"temp-chart__hr t t_c_23\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722103200\">21</p><div class=\"temp-chart__item temp-chart__item_diff_7\"><div class=\"temp-chart__temp\" data-t=\"21\">+21</div><hr class=\"temp-chart__hr t t_c_21\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722106800\">22</p><div class=\"temp-chart__item temp-chart__item_diff_9\"><div class=\"temp-chart__temp\" data-t=\"19\">+19</div><hr class=\"temp-chart__hr t t_c_19\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722110400\">23</p><div class=\"temp-chart__item temp-chart__item_diff_9\"><div class=\"temp-chart__temp\" data-t=\"19\">+19</div><hr class=\"temp-chart__hr t t_c_19\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div><div class=\"temp-chart__wrap\"><p class=\"temp-chart__hour\" data-time=\"1722114000\">0 ч</p><div class=\"temp-chart__item temp-chart__item_diff_10\"><div class=\"temp-chart__temp\" data-t=\"18\">+18</div><hr class=\"temp-chart__hr t t_c_18\"/><i class=\"icon icon_size_24\" aria-hidden=\"true\" data-width=\"24\"></i></div></div></div></div></div></div><div class=\"container__row\"><div class=\"footer clearfix\" role=\"contentinfo\"><div class=\"footer__bigweather\"><a class=\"link link_theme_normal i-bem\" tabindex=\"3\" href=\"//yandex.ru/pogoda/?lat=56.859561&amp;lon=35.911851\" data-bem='{\"link\":{}}'>Погода на 10 дней</a><span class=\"theme-switcher i-bem\" tabindex=\"4\" data-bem='{\"theme-switcher\":{}}'>б/ч</span><span class=\"system-switcher i-bem\" tabindex=\"5\" data-bem='{\"system-switcher\":{}}'>C/F</span><span class=\"footer__updated\">Обновлено в <span class=\"footer__updated-time\">09:00</span></span></div><div class=\"footer__foreca\"><span class=\"footer__row\"><a class=\"link link_theme_normal i-bem\" tabindex=\"0\" href=\"//yandex.ru/legal/weather_termsofuse/\" data-bem='{\"link\":{}}'>Пользовательское соглашение</a>.</span><span class=\"footer__row\"><div class=\"copyright\">© <span class=\"copyright__dates\">2000–2024</span>  ООО «<a class=\"copyright__link\" href=\"//ya.ru\">ЯНДЕКС</a>»</div></span></div></div></div><script src=\"//yastatic.net/jquery/3.3.1/jquery.min.js\"></script><script src=\"//yastatic.net/s3/weather-frontend/302/mini.bundles/index/_index.ru.js\"></script><i class=\"b-statcounter\"><i class=\"b-statcounter__metrika b-statcounter__metrika_type_js i-bem i-bem\" data-bem='{\"b-statcounter__metrika\":{\"enableAll\":true,\"webvisor\":true,\"params\":{\"lang\":\"ru\",\"test_id\":[\"1072440\",\"1057497\",\"299061\",\"299102\"]},\"experiments\":\"MHf5BL6mO1HWIfrmg2ll4U-VDky9RhGDCIJQ6mBM5r9QOasQP1nyho6J1qZM3whQqRG-p1DKra8\",\"id\":5212426}}'><noscript><img alt=\"\" src=\"//mc.yandex.ru/watch/5212426\"/></noscript></i></i></body></html>";

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private NetworkUtils networkUtils;

    @InjectMocks
    private Retrieve retrieve;

    @Test
    void parseWithoutArgumentsTest() {
        BotRequest request = TestUtils.getRequestFromGroup("retrieve");

        List<BotResponse> botResponses = retrieve.parse(request);

        assertTrue(botResponses.isEmpty());
        verify(bot, never()).sendTyping(anyLong());
    }

    @Test
    void parseWithNotEnoughArgumentsTest() {
        final String errorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("retrieve test");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(errorText);

        BotException botException = assertThrows((BotException.class), () -> retrieve.parse(request));

        assertEquals(errorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithCorruptedUrlTest() {
        final String errorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("retrieve test " + PATTERN);

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(errorText);

        BotException botException = assertThrows((BotException.class), () -> retrieve.parse(request));

        assertEquals(errorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithCorruptedPatternTest() {
        final String errorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("retrieve " + URL + " (temp:(\\d+)");

        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(errorText);

        BotException botException = assertThrows((BotException.class), () -> retrieve.parse(request));

        assertEquals(errorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseWithReadUrlExceptionTest() throws IOException {
        final String errorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("retrieve " + URL + " " + PATTERN);

        when(networkUtils.readStringFromURL(any(java.net.URL.class))).thenThrow(new RuntimeException(errorText));

        BotException botException = assertThrows((BotException.class), () -> retrieve.parse(request));

        assertEquals(errorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseFoundNothingTest() throws IOException {
        final String errorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("retrieve " + URL + " 1234");

        when(networkUtils.readStringFromURL(any(java.net.URL.class))).thenReturn(DATA);
        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(errorText);

        BotException botException = assertThrows((BotException.class), () -> retrieve.parse(request));

        assertEquals(errorText, botException.getMessage());
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void parseTest(String pattern, String expected) throws IOException {
        BotRequest request = TestUtils.getRequestFromGroup("retrieve " + URL + " " + pattern);

        when(networkUtils.readStringFromURL(any(java.net.URL.class))).thenReturn(DATA);

        BotResponse botResponse = retrieve.parse(request).get(0);

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expected, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
            Arguments.of("\"temp\":(-?\\d+)", "24\n\n"),
            Arguments.of("data-t=\"(\\d+)\"", "25\n\n26\n\n27\n\n27\n\n28\n\n28\n\n27\n\n26\n\n25\n\n23\n\n21\n\n19\n\n19\n\n18\n\n"),
            Arguments.of("data-t=\"(\\d+)\">(\\+\\d+)", "25\n+25\n\n26\n+26\n\n27\n+27\n\n27\n+27\n\n28\n+28\n\n28\n+28\n\n27\n+27\n\n26\n+26\n\n25\n+25\n\n23\n+23\n\n21\n+21\n\n19\n+19\n\n19\n+19\n\n18\n+18\n\n")
        );
    }

}