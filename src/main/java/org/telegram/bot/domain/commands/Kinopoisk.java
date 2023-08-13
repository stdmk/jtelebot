package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.bot.utils.DateUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Kinopoisk implements CommandParent<PartialBotApiMethod<?>> {

    private final Bot bot;
    private final SpeechService speechService;
    private final PropertiesConfig propertiesConfig;
    private final RestTemplate botRestTemplate;
    private final BotStats botStats;

    private static final Pattern YEAR_PARAM = Pattern.compile("\\((\\d{4})\\)");
    private static final String API_URL = "https://api.kinopoisk.dev/v1.3/movie";
    private static final String KINOPOISK_URL = "https://www.kinopoisk.ru";
    private static final String SEARCH_PATH = "?";
    private static final String GET_BY_ID_PATH = "/";
    private static final String RANDOM_MOVIE_PATH = "/random";

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        String token = propertiesConfig.getKinopoiskToken();
        if (StringUtils.isEmpty(token)) {
            log.error("Unable to find kinopoisk token");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        String textMessage = getTextMessage(update);

        Pair<String, InputFile> result;
        if (textMessage == null) {
            bot.sendUploadPhoto(chatId);
            result = getRandomMovie(token);
        } else if (textMessage.startsWith("_")) {
            bot.sendUploadPhoto(chatId);
            result = getMovieById(token, textMessage);
        } else {
            bot.sendTyping(chatId);
            result = getMovieSearchResult(token, textMessage);
        }

        String responseText = result.getLeft();
        InputFile photo = result.getRight();

        if (photo != null) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(photo);
            sendPhoto.setCaption(responseText);
            sendPhoto.setParseMode("HTML");
            sendPhoto.setReplyToMessageId(message.getMessageId());
            sendPhoto.setChatId(chatId.toString());

            return sendPhoto;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private Pair<String, InputFile> getMovieSearchResult(String token, String textMessage) {
        String url;
        Matcher yearMatcher = YEAR_PARAM.matcher(textMessage);
        if (yearMatcher.find()) {
            int year;
            try {
                year = Integer.parseInt(yearMatcher.group(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            String movieName = textMessage.replaceAll(YEAR_PARAM.pattern(), "").trim();
            url = API_URL + SEARCH_PATH + "name=" + movieName + "&year=" + year;
        } else {
            url = API_URL + SEARCH_PATH + "name=" + textMessage;
        }

        MovieSearchResult movieSearchResult = getData(url, token, MovieSearchResult.class);

        Integer total = movieSearchResult.getTotal();
        if (total == 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        } else if (total == 1) {
            Movie movie = movieSearchResult.getDocs().get(0);

            Long id = movie.getId();
            if (id != null) {
                return getMovieById(token, id);
            } else {
                return getResponseTextAndPhotoFromMovie(movie);
            }
        } else {
            return Pair.of(generateResponseTextToMovies(movieSearchResult), null);
        }
    }

    private Pair<String, InputFile> getMovieById(String token, String textMessage) {
        long id;
        try {
            id = Long.parseLong(textMessage.substring(1));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return getMovieById(token, id);
    }

    private Pair<String, InputFile> getMovieById(String token, Long id) {
        Movie movie = getData(API_URL + GET_BY_ID_PATH + id, token, Movie.class);
        return getResponseTextAndPhotoFromMovie(movie);
    }

    private Pair<String, InputFile> getRandomMovie(String token) {
        Movie movie = getData(API_URL + RANDOM_MOVIE_PATH, token, Movie.class);
        return getResponseTextAndPhotoFromMovie(movie);
    }

    private Pair<String, InputFile> getResponseTextAndPhotoFromMovie(Movie movie) {
        String responseText = generateResponseTextToMovie(movie);
        InputFile photo = getPhotoFromMovie(movie);

        return Pair.of(responseText, photo);
    }

    private String generateResponseTextToMovies(MovieSearchResult movieSearchResult) {
        StringBuilder buf = new StringBuilder();

        movieSearchResult.getDocs().forEach(movie -> {
            buf.append("<b>").append(movie.getName()).append("</b>").append(" (").append(movie.getYear()).append(")\n");
            ifPresentAndNotEmpty(movie.getShortDescription(), shortDescription ->
                    buf.append(shortDescription).append("\n"));
            buf.append("/movie_").append(movie.getId()).append("\n\n");
        });
        buf.append("Всего найдено: <b>").append(movieSearchResult.getTotal()).append("</b>\n");

        return buf.toString();
    }

    private String generateResponseTextToMovie(Movie movie) {
        StringBuilder buf = new StringBuilder();

        buf.append("<b>").append(movie.getName()).append(" (").append(movie.getYear()).append(")</b>\n");
        ifPresentAndNotEmpty(movie.getAlternativeName(), buf::append);
        ifPresentAndNotEmpty(movie.getAgeRating(), age -> buf.append(" ").append(age).append("+\n"));
        buf.append("\n");
        ifPresentAndNotEmpty(movie.getMovieLength(), movieLength ->
                buf.append("Продолжительность: <b>").append(movieLength).append(" м.</b> (")
                        .append(DateUtils.durationToString(movieLength * 60 * 1000)).append(")\n"));
        ifPresentAndNotEmpty(movie.getTop10(), top10 ->
                buf.append(Emoji.TROPHY.getEmoji()).append(" топ10 (").append(top10).append(")\n"));
        ifPresentAndNotEmpty(movie.getTop250(), top250 ->
                buf.append(Emoji.TROPHY.getEmoji()).append(" топ250 (").append(top250).append(")\n"));
        ifPresentAndNotEmpty(movie.getRating(), rating -> {
            ifPresentAndNotEmpty(rating.getKp(), kp -> buf.append("Кинопоиск: <b>").append(kp).append("</b> "));
            ifPresentAndNotEmpty(rating.getImdb(), imdb -> buf.append("IMDB: <b>").append(imdb).append("</b> "));
            buf.append("\n");
        });
        ifPresentAndNotEmpty(movie.getCountries(), countries ->
                buf.append("Страна: <b>").append(getNames(countries)).append("</b>\n"));
        ifPresentAndNotEmpty(movie.getGenres(), genres ->
                buf.append("Жанр: <b>").append(getNames(genres)).append("</b>\n"));
        ifPresentAndNotEmpty(movie.getPersons(), persons -> persons
                .stream()
                .filter(person -> "director".equals(person.getEnProfession()))
                .findFirst()
                .ifPresent(director -> buf.append("Режиссёр: <b>").append(director.getName()).append("</b>\n")));
        ifPresentAndNotEmpty(movie.getShortDescription(), description ->
                buf.append("\n").append("<i>").append(description).append("</i>\n"));
        buf.append("\n");
        ifPresentAndNotEmpty(movie.getPersons(), persons -> {
            buf.append("В ролях: ");

            persons
                    .stream()
                    .filter(person -> "actor".equals(person.getEnProfession()))
                    .limit(9)
                    .map(person -> person.getName() == null ? person.getEnName() : person.getName())
                    .forEach(name -> buf.append(name).append(", "));
            if (persons.size() >= 10) {
                buf.append(persons.get(9).getName());
            }

            buf.append("\n\n");
        });
        ifPresentAndNotEmpty(movie.getVideos(), videos -> {
            List<Video> videosList = new ArrayList<>();

            ifPresentAndNotEmpty(videos.getTrailers(), videosList::addAll);
            ifPresentAndNotEmpty(videos.getTeasers(), videosList::addAll);

            if (!CollectionUtils.isEmpty(videosList)) {
                buf.append("Трейлеры: \n");
                int i = 0;
                for (Video video : videosList) {
                    if (i > 9) {
                        break;
                    }
                    i = i + 1;
                    buf.append("<a href='").append(video.getUrl()).append("'>").append(i).append(" </a>");
                }

                buf.append("\n\n");
            }
        });
        ifPresentAndNotEmpty(movie.getSimilarMovies(), similarMovies -> {
            buf.append("Похожее:\n");
            similarMovies.stream().limit(5).forEach(similarMovie ->
                    buf.append("/movie_").append(similarMovie.getId()).append(" — ").append(similarMovie.getName()).append("\n"));
        });

        buf.append("<a href='" + KINOPOISK_URL + "/film/").append(movie.getId()).append("'>На сайт</a>");

        return buf.toString();
    }

    private String getNames(List<ItemName> names) {
        return names.stream().map(ItemName::getName).collect(Collectors.joining(", "));
    }

    private InputFile getPhotoFromMovie(Movie movie) {
        return Optional.ofNullable(movie.getPoster())
                .map(ShortImage::getUrl)
                .map(InputFile::new)
                .orElse(null);
    }

    private <T> T getData(String url, String token, Class<T> dataType) {
        ResponseEntity<T> responseEntity;
        try {
            responseEntity = botRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(token)), dataType);
        } catch (HttpClientErrorException hcee) {
            String response = hcee.getResponseBodyAsString(StandardCharsets.UTF_8);

            response = response.substring(response.indexOf("\"message\":\"") + 11);
            String errorText = response.substring(0, response.indexOf("\""));

            throw new BotException("Ошибка от сервиса: " + errorText);
        } catch (RestClientException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementKinopoiskRequests();

        T value = responseEntity.getBody();

        if (value == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return value;
    }

    private HttpHeaders getDefaultHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-API-KEY", token);

        return headers;
    }

    private <T> void ifPresentAndNotEmpty(T value, Consumer<? super T> action) {
        if (ObjectUtils.isNotEmpty(value)) {
            action.accept(value);
        }
    }

    @Data
    private static class MovieSearchResult {
        private List<Movie> docs;
        private Integer total;
        private Integer limit;
        private Integer page;
        private Integer pages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Movie {
        private Long id;
        private String name;
        private String alternativeName;
        private String enName;
        private List<Name> names;
        private String type;
        private Integer year;
        private String description;
        private String shortDescription;
        private String slogan;
        private String status;
        private Rating rating;
        private Votes votes;
        private Integer movieLength;
        private String ratingMpaa;
        private Integer ageRating;
        private ShortImage poster;
        private VideoTypes videos;
        private List<ItemName> genres;
        private List<ItemName> countries;
        private List<YearRange> releaseYears;
        private List<PersonInMovie> persons;
        List<LinkedMovie> similarMovies;
        private Integer top10;
        private Integer top250;
    }

    @Data
    private static class Name {
        private String name;
        private String language;
        private String type;
    }

    @Data
    private static class Rating {
        private Double kp;
        private Double imdb;
        private Double tmdb;
        private Double filmCritics;
        private Double russianFilmCritics;
        private Double await;
    }

    @Data
    private static class Votes {
        private String kp;
        private String imdb;
        private Double tmdb;
        private Double filmCritics;
        private Double russianFilmCritics;
        private Double await;
    }

    @Data
    private static class ShortImage {
        private String url;
        private String previewUrl;
    }

    @Data
    private static class VideoTypes {
        private List<Video> trailers;
        private List<Video> teasers;
    }

    @Data
    private static class Video {
        private String url;
        private String name;
        private String site;
        private String type;
        private BigDecimal size;
    }

    @Data
    private static class ItemName {
        private String name;
    }

    @Data
    private static class YearRange {
        private Integer start;
        private Integer end;
    }

    @Data
    private static class PersonInMovie {
        private BigDecimal id;
        private String photo;
        private String name;
        private String enName;
        private String description;
        private String profession;
        private String enProfession;
    }

    @Data
    private static class LinkedMovie {
        private Long id;
        private String name;
        private String enName;
        private String alternativeName;
        private String type;
        private ShortImage poster;
    }
}
