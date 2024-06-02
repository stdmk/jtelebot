package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.utils.DateUtils;

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
public class Kinopoisk implements Command {

    private final Bot bot;
    private final SpeechService speechService;
    private final PropertiesConfig propertiesConfig;
    private final RestTemplate botRestTemplate;
    private final BotStats botStats;

    private static final Pattern YEAR_PARAM = Pattern.compile("\\((\\d{4})\\)");
    private static final String API_URL = "https://api.kinopoisk.dev/v1.3/movie";
    private static final String KINOPOISK_URL = "https://www.kinopoisk.ru/";
    private static final String SEARCH_PATH = "?";
    private static final String RANDOM_MOVIE_PATH = "random";

    @Override
    public List<BotResponse> parse(BotRequest request) {
        String token = propertiesConfig.getKinopoiskToken();
        if (StringUtils.isEmpty(token)) {
            log.error("Unable to find kinopoisk token");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = request.getMessage();
        Long chatId = message.getChatId();
        String commandArgument = message.getCommandArgument();

        MovieData movieData;
        if (commandArgument == null) {
            bot.sendUploadPhoto(chatId);
            movieData = getRandomMovie(token);
        } else if (commandArgument.startsWith("_")) {
            bot.sendUploadPhoto(chatId);
            movieData = getMovieById(token, commandArgument);
        } else {
            bot.sendTyping(chatId);
            movieData = getMovieSearchResult(token, commandArgument);
        }

        String responseText = movieData.getText();
        String photoUrl = movieData.getPhotoUrl();

        if (photoUrl != null) {
            return returnResponse(new FileResponse(message)
                    .setText(responseText)
                    .addFile(new File(FileType.IMAGE, photoUrl))
                    .setResponseSettings(FormattingStyle.HTML));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setFormattingStyle(FormattingStyle.HTML)
                        .setWebPagePreview(false)));
    }

    private MovieData getMovieSearchResult(String token, String textMessage) {
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
            return new MovieData(generateResponseTextToMovies(movieSearchResult), null);
        }
    }

    private MovieData getMovieById(String token, String textMessage) {
        long id;
        try {
            id = Long.parseLong(textMessage.substring(1));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return getMovieById(token, id);
    }

    private MovieData getMovieById(String token, Long id) {
        Movie movie = getData(API_URL + "/" + id, token, Movie.class);
        return getResponseTextAndPhotoFromMovie(movie);
    }

    private MovieData getRandomMovie(String token) {
        Movie movie = getData(API_URL + RANDOM_MOVIE_PATH, token, Movie.class);
        return getResponseTextAndPhotoFromMovie(movie);
    }

    private MovieData getResponseTextAndPhotoFromMovie(Movie movie) {
        String responseText = generateResponseTextToMovie(movie);
        String urlOfPhoto = getUrlOfPhotoFromMovie(movie);

        return new MovieData(responseText, urlOfPhoto);
    }

    private String generateResponseTextToMovies(MovieSearchResult movieSearchResult) {
        StringBuilder buf = new StringBuilder();

        movieSearchResult.getDocs().forEach(movie -> {
            buf.append("<b>").append(movie.getName()).append("</b>").append(" (").append(movie.getYear()).append(")\n");
            ifPresentAndNotEmpty(movie.getShortDescription(), shortDescription ->
                    buf.append(shortDescription).append("\n"));
            buf.append("/movie_").append(movie.getId()).append("\n\n");
        });
        buf.append("${command.kinopoisk.totalfound}: <b>").append(movieSearchResult.getTotal()).append("</b>\n");

        return buf.toString();
    }

    private String generateResponseTextToMovie(Movie movie) {
        StringBuilder buf = new StringBuilder();

        buf.append("<b>").append(movie.getName()).append(" (").append(movie.getYear()).append(")</b>\n");
        ifPresentAndNotEmpty(movie.getAlternativeName(), buf::append);
        ifPresentAndNotEmpty(movie.getAgeRating(), age -> buf.append(" ").append(age).append("+\n"));
        buf.append("\n");
        ifPresentAndNotEmpty(movie.getMovieLength(), movieLength ->
                buf.append("${command.kinopoisk.movieinfo.duration}: <b>").append(movieLength).append(" ${command.kinopoisk.movieinfo.minute}.</b> (")
                        .append(DateUtils.durationToString(movieLength * 60 * 1000)).append(")\n"));
        ifPresentAndNotEmpty(movie.getTop10(), top10 ->
                buf.append(Emoji.TROPHY.getSymbol()).append(" ${command.kinopoisk.movieinfo.top10} (").append(top10).append(")\n"));
        ifPresentAndNotEmpty(movie.getTop250(), top250 ->
                buf.append(Emoji.TROPHY.getSymbol()).append(" ${command.kinopoisk.movieinfo.top250} (").append(top250).append(")\n"));
        ifPresentAndNotEmpty(movie.getRating(), rating -> {
            ifPresentAndNotEmpty(rating.getKp(), kp -> buf.append("${command.kinopoisk.movieinfo.kinopoiskrating}: <b>").append(kp).append("</b> "));
            ifPresentAndNotEmpty(rating.getImdb(), imdb -> buf.append("${command.kinopoisk.movieinfo.imdbrating}: <b>").append(imdb).append("</b> "));
            buf.append("\n");
        });
        ifPresentAndNotEmpty(movie.getCountries(), countries ->
                buf.append("${command.kinopoisk.movieinfo.country}: <b>").append(getNames(countries)).append("</b>\n"));
        ifPresentAndNotEmpty(movie.getGenres(), genres ->
                buf.append("${command.kinopoisk.movieinfo.genre}: <b>").append(getNames(genres)).append("</b>\n"));
        ifPresentAndNotEmpty(movie.getPersons(), persons -> persons
                .stream()
                .filter(person -> "director".equals(person.getEnProfession()))
                .findFirst()
                .ifPresent(director -> buf.append("${command.kinopoisk.movieinfo.director}: <b>").append(director.getName()).append("</b>\n")));
        ifPresentAndNotEmpty(movie.getShortDescription(), description ->
                buf.append("\n").append("<i>").append(description).append("</i>\n"));
        buf.append("\n");
        ifPresentAndNotEmpty(movie.getPersons(), persons -> {
            buf.append("${command.kinopoisk.movieinfo.actors}: ");

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
                buf.append("${command.kinopoisk.movieinfo.trailers}: \n");
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
            buf.append("${command.kinopoisk.movieinfo.similar}:\n");
            similarMovies.stream().limit(5).forEach(similarMovie ->
                    buf.append("/movie_").append(similarMovie.getId()).append(" — ").append(similarMovie.getName()).append("\n"));
        });

        Long movieId = movie.getId();
        if (movieId != null) {
            buf.append("<a href='" + KINOPOISK_URL + "/film/").append(movieId).append("'>${command.kinopoisk.movieinfo.towebsite}</a>");
        }

        return buf.toString();
    }

    private String getNames(List<ItemName> names) {
        return names.stream().map(ItemName::getName).collect(Collectors.joining(", "));
    }

    private String getUrlOfPhotoFromMovie(Movie movie) {
        return Optional.ofNullable(movie.getPoster())
                .map(ShortImage::getUrl)
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

            throw new BotException("${command.kinopoisk.errorfromapi}: " + errorText);
        } catch (RestClientException e) {
            log.error("Unable to get kinopoisk result", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementKinopoiskRequests();

        T value = responseEntity.getBody();

        if (value == null) {
            log.error("Empty response body from kinopoisk");
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

    @Value
    public static class MovieData {
        String text;
        String photoUrl;
    }

    @Data
    @Accessors(chain = true)
    public static class MovieSearchResult {
        private List<Movie> docs;
        private Integer total;
        private Integer limit;
        private Integer page;
        private Integer pages;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Movie {
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
        private List<LinkedMovie> similarMovies;
        private Integer top10;
        private Integer top250;
    }

    @Data
    @Accessors(chain = true)
    public static class Name {
        private String name;
        private String language;
        private String type;
    }

    @Data
    @Accessors(chain = true)
    public static class Rating {
        private Double kp;
        private Double imdb;
        private Double tmdb;
        private Double filmCritics;
        private Double russianFilmCritics;
        private Double await;
    }

    @Data
    @Accessors(chain = true)
    public static class Votes {
        private String kp;
        private String imdb;
        private Double tmdb;
        private Double filmCritics;
        private Double russianFilmCritics;
        private Double await;
    }

    @Data
    @Accessors(chain = true)
    public static class ShortImage {
        private String url;
        private String previewUrl;
    }

    @Data
    @Accessors(chain = true)
    public static class VideoTypes {
        private List<Video> trailers;
        private List<Video> teasers;
    }

    @Data
    @Accessors(chain = true)
    public static class Video {
        private String url;
        private String name;
        private String site;
        private String type;
        private BigDecimal size;
    }

    @Data
    @Accessors(chain = true)
    public static class ItemName {
        private String name;
    }

    @Data
    @Accessors(chain = true)
    public static class YearRange {
        private Integer start;
        private Integer end;
    }

    @Data
    @Accessors(chain = true)
    public static class PersonInMovie {
        private BigDecimal id;
        private String photo;
        private String name;
        private String enName;
        private String description;
        private String profession;
        private String enProfession;
    }

    @Data
    @Accessors(chain = true)
    public static class LinkedMovie {
        private Long id;
        private String name;
        private String enName;
        private String alternativeName;
        private String type;
        private ShortImage poster;
    }
}
