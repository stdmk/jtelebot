package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KinopoiskTest {

    private static final String API_URL = "https://api.kinopoisk.dev/v1.3/movie";
    private static final String SEARCH_PATH = "?";
    private static final String RANDOM_MOVIE_PATH = "random";
    private static final String TOKEN = "token";
    private static final HttpHeaders DEFAULT_HEADERS = new HttpHeaders(CollectionUtils.toMultiValueMap(
            Map.of("Content-Type", List.of("application/json"), "X-API-KEY", List.of(TOKEN))));
    private static final Long MOVIE_ID = 123L;

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private BotStats botStats;

    @InjectMocks
    private Kinopoisk kinopoisk;

    @Test
    void parseWithoutTokenTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        assertThrows(BotException.class, () -> kinopoisk.parse(request));
        verify(speechService).getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN);
    }

    @Test
    void parseRandomMovieWithApiErrorTest() {
        final String expectedErrorText = "Превышен дневной лимит!";
        String apiErrorResponse = "{\"statusCode\":403,\"message\":\"" + expectedErrorText + "\",\"error\":\"Forbidden\"}";

        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + RANDOM_MOVIE_PATH, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.Movie.class))
                .thenThrow(
                        new HttpClientErrorException(
                                HttpStatus.FORBIDDEN,
                                HttpStatus.FORBIDDEN.getReasonPhrase(),
                                apiErrorResponse.getBytes(StandardCharsets.UTF_8),
                                StandardCharsets.UTF_8));

        BotRequest request = TestUtils.getRequestFromGroup();

        BotException botException = assertThrows(BotException.class, () -> kinopoisk.parse(request));
        assertEquals("${command.kinopoisk.errorfromapi}: " + expectedErrorText, botException.getMessage());
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseRandomMovieWithRestClientExceptionTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + RANDOM_MOVIE_PATH, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.Movie.class))
                .thenThrow(new RestClientException("error"));

        assertThrows(BotException.class, () -> kinopoisk.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseRandomMovieWithEmptyResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + RANDOM_MOVIE_PATH, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.Movie.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertThrows(BotException.class, () -> kinopoisk.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseRandomMovieTest() {
        final String expectedCaption = "<b>name (2000)</b>\n" +
                "alternativeName 21+\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.duration}: <b>120 ${command.kinopoisk.movieinfo.minute}.</b> (2 ${utils.date.h}. )\n" +
                "\uD83C\uDFC6 ${command.kinopoisk.movieinfo.top10} (3)\n" +
                "\uD83C\uDFC6 ${command.kinopoisk.movieinfo.top250} (4)\n" +
                "${command.kinopoisk.movieinfo.kinopoiskrating}: <b>1.0</b> ${command.kinopoisk.movieinfo.imdbrating}: <b>2.0</b> \n" +
                "${command.kinopoisk.movieinfo.country}: <b>country1, country2</b>\n" +
                "${command.kinopoisk.movieinfo.genre}: <b>genres1, genres2</b>\n" +
                "${command.kinopoisk.movieinfo.director}: <b>personName1</b>\n" +
                "\n" +
                "<i>shortDesc</i>\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.actors}: personName2, personName3, personName4, personName5, personName6, personName7, personName8, personName9, personName10, personName10\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.trailers}: \n" +
                "<a href='url1'>1 </a><a href='url2'>2 </a><a href='url3'>3 </a><a href='ur24'>4 </a><a href='url5'>5 </a><a href='url6'>6 </a><a href='url7'>7 </a><a href='url8'>8 </a><a href='url9'>9 </a><a href='ur10'>10 </a>\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.similar}:\n" +
                "/movie_20 — linkedMovieName1\n" +
                "/movie_21 — linkedMovieName2\n" +
                "<a href='https://www.kinopoisk.ru//film/123'>${command.kinopoisk.movieinfo.towebsite}</a>";
        BotRequest request = TestUtils.getRequestFromGroup();
        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + RANDOM_MOVIE_PATH, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.Movie.class))
                .thenReturn(new ResponseEntity<>(getSomeMovie(), HttpStatus.OK));

        BotResponse response = kinopoisk.parse(request).get(0);
        FileResponse photo = TestUtils.checkDefaultFileResponseImageParams(response);
        assertEquals(expectedCaption, photo.getText());

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseMovieByCorruptedIdTest() {
        BotRequest request = TestUtils.getRequestFromGroup("movie_a");
        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");

        assertThrows(BotException.class, () -> kinopoisk.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseMovieByIdTest() {
        final String expectedCaption = "<b>name (2000)</b>\n" +
                "alternativeName 21+\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.duration}: <b>120 ${command.kinopoisk.movieinfo.minute}.</b> (2 ${utils.date.h}. )\n" +
                "\uD83C\uDFC6 ${command.kinopoisk.movieinfo.top10} (3)\n" +
                "\uD83C\uDFC6 ${command.kinopoisk.movieinfo.top250} (4)\n" +
                "${command.kinopoisk.movieinfo.kinopoiskrating}: <b>1.0</b> ${command.kinopoisk.movieinfo.imdbrating}: <b>2.0</b> \n" +
                "${command.kinopoisk.movieinfo.country}: <b>country1, country2</b>\n" +
                "${command.kinopoisk.movieinfo.genre}: <b>genres1, genres2</b>\n" +
                "${command.kinopoisk.movieinfo.director}: <b>personName1</b>\n" +
                "\n" +
                "<i>shortDesc</i>\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.actors}: personName2, personName3, personName4, personName5, personName6, personName7, personName8, personName9, personName10, personName10\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.trailers}: \n" +
                "<a href='url1'>1 </a><a href='url2'>2 </a><a href='url3'>3 </a><a href='ur24'>4 </a><a href='url5'>5 </a><a href='url6'>6 </a><a href='url7'>7 </a><a href='url8'>8 </a><a href='url9'>9 </a><a href='ur10'>10 </a>\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.similar}:\n" +
                "/movie_20 — linkedMovieName1\n" +
                "/movie_21 — linkedMovieName2\n" +
                "<a href='https://www.kinopoisk.ru//film/123'>${command.kinopoisk.movieinfo.towebsite}</a>";
        BotRequest request = TestUtils.getRequestFromGroup("movie_" + MOVIE_ID);

        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + "/" + MOVIE_ID, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.Movie.class))
                .thenReturn(new ResponseEntity<>(getSomeMovie(), HttpStatus.OK));

        BotResponse response = kinopoisk.parse(request).get(0);
        FileResponse photo = TestUtils.checkDefaultFileResponseImageParams(response);
        assertEquals(expectedCaption, photo.getText());

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseSearchMovieFoundNothingTest() {
        final String movieName = "movie name";
        BotRequest request = TestUtils.getRequestFromGroup("movie " + movieName);
        Kinopoisk.MovieSearchResult movieSearchResult = getSomeMovieSearchResult().setTotal(0);

        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + SEARCH_PATH + "name=" + movieName, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.MovieSearchResult.class))
                .thenReturn(new ResponseEntity<>(movieSearchResult, HttpStatus.OK));

        assertThrows(BotException.class, () -> kinopoisk.parse(request));

        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseSearchMovieWithYearFoundOneWithIdTest() {
        final String movieName = "movie name";
        final int movieYear = 2000;
        final String expectedCaption = "<b>name (2000)</b>\n" +
                "alternativeName 21+\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.duration}: <b>120 ${command.kinopoisk.movieinfo.minute}.</b> (2 ${utils.date.h}. )\n" +
                "\uD83C\uDFC6 ${command.kinopoisk.movieinfo.top10} (3)\n" +
                "\uD83C\uDFC6 ${command.kinopoisk.movieinfo.top250} (4)\n" +
                "${command.kinopoisk.movieinfo.kinopoiskrating}: <b>1.0</b> ${command.kinopoisk.movieinfo.imdbrating}: <b>2.0</b> \n" +
                "${command.kinopoisk.movieinfo.country}: <b>country1, country2</b>\n" +
                "${command.kinopoisk.movieinfo.genre}: <b>genres1, genres2</b>\n" +
                "${command.kinopoisk.movieinfo.director}: <b>personName1</b>\n" +
                "\n" +
                "<i>shortDesc</i>\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.actors}: personName2, personName3, personName4, personName5, personName6, personName7, personName8, personName9, personName10, personName10\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.trailers}: \n" +
                "<a href='url1'>1 </a><a href='url2'>2 </a><a href='url3'>3 </a><a href='ur24'>4 </a><a href='url5'>5 </a><a href='url6'>6 </a><a href='url7'>7 </a><a href='url8'>8 </a><a href='url9'>9 </a><a href='ur10'>10 </a>\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.similar}:\n" +
                "/movie_20 — linkedMovieName1\n" +
                "/movie_21 — linkedMovieName2\n" +
                "<a href='https://www.kinopoisk.ru//film/123'>${command.kinopoisk.movieinfo.towebsite}</a>";
        BotRequest request = TestUtils.getRequestFromGroup("movie " + movieName + "(" + movieYear + ")");
        Kinopoisk.MovieSearchResult movieSearchResult = getSomeMovieSearchResult().setTotal(1).setDocs(List.of(getSomeMovie()));

        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + SEARCH_PATH + "name=" + movieName + "&year=" + movieYear, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.MovieSearchResult.class))
                .thenReturn(new ResponseEntity<>(movieSearchResult, HttpStatus.OK));
        when(botRestTemplate.exchange(API_URL + "/" + MOVIE_ID, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.Movie.class))
                .thenReturn(new ResponseEntity<>(getSomeMovie(), HttpStatus.OK));

        BotResponse response = kinopoisk.parse(request).get(0);
        FileResponse photo = TestUtils.checkDefaultFileResponseImageParams(response);
        assertEquals(expectedCaption, photo.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseSearchMovieFoundOneWithoutIdTest() {
        final String movieName = "movie name";
        final int movieYear = 2000;
        final String expectedCaption = "<b>name (2000)</b>\n" +
                "alternativeName 21+\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.duration}: <b>120 ${command.kinopoisk.movieinfo.minute}.</b> (2 ${utils.date.h}. )\n" +
                "\uD83C\uDFC6 ${command.kinopoisk.movieinfo.top10} (3)\n" +
                "\uD83C\uDFC6 ${command.kinopoisk.movieinfo.top250} (4)\n" +
                "${command.kinopoisk.movieinfo.kinopoiskrating}: <b>1.0</b> ${command.kinopoisk.movieinfo.imdbrating}: <b>2.0</b> \n" +
                "${command.kinopoisk.movieinfo.country}: <b>country1, country2</b>\n" +
                "${command.kinopoisk.movieinfo.genre}: <b>genres1, genres2</b>\n" +
                "${command.kinopoisk.movieinfo.director}: <b>personName1</b>\n" +
                "\n" +
                "<i>shortDesc</i>\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.actors}: personName2, personName3, personName4, personName5, personName6, personName7, personName8, personName9, personName10, personName10\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.trailers}: \n" +
                "<a href='url1'>1 </a><a href='url2'>2 </a><a href='url3'>3 </a><a href='ur24'>4 </a><a href='url5'>5 </a><a href='url6'>6 </a><a href='url7'>7 </a><a href='url8'>8 </a><a href='url9'>9 </a><a href='ur10'>10 </a>\n" +
                "\n" +
                "${command.kinopoisk.movieinfo.similar}:\n" +
                "/movie_20 — linkedMovieName1\n" +
                "/movie_21 — linkedMovieName2\n";
        BotRequest request = TestUtils.getRequestFromGroup("movie " + movieName + "(" + movieYear + ")");
        Kinopoisk.Movie foundMovie = getSomeMovie().setId(null);
        Kinopoisk.MovieSearchResult movieSearchResult = getSomeMovieSearchResult().setTotal(1).setDocs(List.of(foundMovie));

        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + SEARCH_PATH + "name=" + movieName + "&year=" + movieYear, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.MovieSearchResult.class))
                .thenReturn(new ResponseEntity<>(movieSearchResult, HttpStatus.OK));

        BotResponse response = kinopoisk.parse(request).get(0);
        FileResponse photo = TestUtils.checkDefaultFileResponseImageParams(response);
        assertEquals(expectedCaption, photo.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    @Test
    void parseSearchMovieFoundSeveralMoviesTest() {
        final String movieName = "movie name";
        final String expectedResponseText = "<b>name</b> (2000)\n" +
                "shortDesc\n" +
                "/movie_123\n" +
                "\n" +
                "<b>name</b> (2000)\n" +
                "shortDesc\n" +
                "/movie_124\n" +
                "\n" +
                "${command.kinopoisk.totalfound}: <b>2</b>\n";
        BotRequest request = TestUtils.getRequestFromGroup("movie " + movieName);
        Kinopoisk.Movie foundMovie1 = getSomeMovie();
        Kinopoisk.Movie foundMovie2 = getSomeMovie().setId(124L);
        Kinopoisk.MovieSearchResult movieSearchResult = getSomeMovieSearchResult().setTotal(2).setDocs(List.of(foundMovie1, foundMovie2));

        when(propertiesConfig.getKinopoiskToken()).thenReturn("token");
        when(botRestTemplate.exchange(API_URL + SEARCH_PATH + "name=" + movieName, HttpMethod.GET, new HttpEntity<>(DEFAULT_HEADERS), Kinopoisk.MovieSearchResult.class))
                .thenReturn(new ResponseEntity<>(movieSearchResult, HttpStatus.OK));

        BotResponse response = kinopoisk.parse(request).get(0);
        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(response);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(request.getMessage().getChatId());
    }

    private Kinopoisk.MovieSearchResult getSomeMovieSearchResult() {
        return new Kinopoisk.MovieSearchResult()
                .setPage(1)
                .setLimit(100)
                .setPages(10)
                .setDocs(List.of());
    }

    private Kinopoisk.Movie getSomeMovie() {
        return new Kinopoisk.Movie()
                .setId(MOVIE_ID)
                .setName("name")
                .setAlternativeName("alternativeName")
                .setEnName("enName")
                .setNames(List.of(
                        new Kinopoisk.Name().setName("name1").setLanguage("language1").setType("type1"),
                        new Kinopoisk.Name().setName("name2").setLanguage("language2").setType("type2")))
                .setType("type")
                .setYear(2000)
                .setDescription("description")
                .setShortDescription("shortDesc")
                .setSlogan("slogan")
                .setStatus("status")
                .setRating(new Kinopoisk.Rating().setKp(1.0).setImdb(2.0).setTmdb(3.0).setAwait(4.0).setFilmCritics(5.0).setRussianFilmCritics(6.0))
                .setVotes(new Kinopoisk.Votes().setKp("7").setImdb("8").setTmdb(9.0).setAwait(10.0).setFilmCritics(11.0).setRussianFilmCritics(12.0))
                .setMovieLength(120)
                .setRatingMpaa("ratingMpaa")
                .setAgeRating(21)
                .setPoster(new Kinopoisk.ShortImage().setUrl("url").setPreviewUrl("previewUrl"))
                .setVideos(new Kinopoisk.VideoTypes()
                        .setTrailers(List.of(
                                new Kinopoisk.Video().setName("name1").setUrl("url1").setType("type1").setSite("site1").setSize(new BigDecimal(50)),
                                new Kinopoisk.Video().setName("name2").setUrl("url2").setType("type2").setSite("site2").setSize(new BigDecimal(51)),
                                new Kinopoisk.Video().setName("name3").setUrl("url3").setType("type3").setSite("site3").setSize(new BigDecimal(52)),
                                new Kinopoisk.Video().setName("name4").setUrl("ur24").setType("type4").setSite("site4").setSize(new BigDecimal(53)),
                                new Kinopoisk.Video().setName("name5").setUrl("url5").setType("type5").setSite("site5").setSize(new BigDecimal(54)),
                                new Kinopoisk.Video().setName("name6").setUrl("url6").setType("type6").setSite("site6").setSize(new BigDecimal(55)),
                                new Kinopoisk.Video().setName("name7").setUrl("url7").setType("type7").setSite("site7").setSize(new BigDecimal(56)),
                                new Kinopoisk.Video().setName("name8").setUrl("url8").setType("type8").setSite("site8").setSize(new BigDecimal(57)),
                                new Kinopoisk.Video().setName("name9").setUrl("url9").setType("type9").setSite("site9").setSize(new BigDecimal(58)),
                                new Kinopoisk.Video().setName("name10").setUrl("ur10").setType("type10").setSite("site10").setSize(new BigDecimal(59))))
                        .setTeasers(List.of(
                                new Kinopoisk.Video().setName("name3").setUrl("url3").setType("type3").setSite("site3").setSize(new BigDecimal(62)),
                                new Kinopoisk.Video().setName("name4").setUrl("url4").setType("type3").setSite("site4").setSize(new BigDecimal(63)))))
                .setGenres(List.of(
                        new Kinopoisk.ItemName().setName("genres1"),
                        new Kinopoisk.ItemName().setName("genres2")))
                .setCountries(List.of(
                        new Kinopoisk.ItemName().setName("country1"),
                        new Kinopoisk.ItemName().setName("country2")))
                .setReleaseYears(List.of(
                        new Kinopoisk.YearRange().setStart(2001).setEnd(2002),
                        new Kinopoisk.YearRange().setStart(2003).setEnd(2004)))
                .setPersons(List.of(
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(1))
                                .setName("personName1")
                                .setEnName("personEnName1")
                                .setDescription("personDesc1")
                                .setPhoto("personPhoto1")
                                .setProfession("profession1")
                                .setEnProfession("director"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(2))
                                .setName("personName2")
                                .setEnName("personEnName2")
                                .setDescription("personDesc2")
                                .setPhoto("personPhoto2")
                                .setProfession("profession2")
                                .setEnProfession("actor"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(3))
                                .setName("personName3")
                                .setEnName("personEnName3")
                                .setDescription("personDesc3")
                                .setPhoto("personPhoto3")
                                .setProfession("profession3")
                                .setEnProfession("actor"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(4))
                                .setName("personName4")
                                .setEnName("personEnName4")
                                .setDescription("personDesc4")
                                .setPhoto("personPhoto4")
                                .setProfession("profession4")
                                .setEnProfession("actor"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(5))
                                .setName("personName5")
                                .setEnName("personEnName5")
                                .setDescription("personDesc5")
                                .setPhoto("personPhoto5")
                                .setProfession("profession5")
                                .setEnProfession("actor"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(6))
                                .setName("personName6")
                                .setEnName("personEnName6")
                                .setDescription("personDesc6")
                                .setPhoto("personPhoto6")
                                .setProfession("profession6")
                                .setEnProfession("actor"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(7))
                                .setName("personName7")
                                .setEnName("personEnName7")
                                .setDescription("personDesc7")
                                .setPhoto("personPhoto7")
                                .setProfession("profession7")
                                .setEnProfession("actor"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(8))
                                .setName("personName8")
                                .setEnName("personEnName8")
                                .setDescription("personDesc8")
                                .setPhoto("personPhoto8")
                                .setProfession("profession8")
                                .setEnProfession("actor"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(9))
                                .setName("personName9")
                                .setEnName("personEnName9")
                                .setDescription("personDesc9")
                                .setPhoto("personPhoto9")
                                .setProfession("profession9")
                                .setEnProfession("actor"),
                        new Kinopoisk.PersonInMovie()
                                .setId(new BigDecimal(10))
                                .setName("personName10")
                                .setEnName("personEnName10")
                                .setDescription("personDesc10")
                                .setPhoto("personPhoto10")
                                .setProfession("profession10")
                                .setEnProfession("actor")
                        ))
                .setSimilarMovies(List.of(
                        new Kinopoisk.LinkedMovie()
                                .setId(20L)
                                .setName("linkedMovieName1")
                                .setEnName("linkedMovieEnName1")
                                .setAlternativeName("linkedMovieAlternativeName1")
                                .setType("linkedMovieType1")
                                .setPoster(new Kinopoisk.ShortImage()
                                        .setUrl("linkedMoviePosterUrl1")
                                        .setPreviewUrl("linkedMoviePosterPreviewUrl1")),
                        new Kinopoisk.LinkedMovie()
                                .setId(21L)
                                .setName("linkedMovieName2")
                                .setEnName("linkedMovieEnName2")
                                .setAlternativeName("linkedMovieAlternativeName2")
                                .setType("linkedMovieType2")
                                .setPoster(new Kinopoisk.ShortImage()
                                        .setUrl("linkedMoviePosterUrl2")
                                        .setPreviewUrl("linkedMoviePosterPreviewUrl2"))))
                .setTop10(3)
                .setTop250(4);
    }

}