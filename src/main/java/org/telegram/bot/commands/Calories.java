package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.*;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.mapper.caloric.CaloricMapper;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.calories.*;
import org.telegram.bot.utils.DateUtils;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.BORDER;

@RequiredArgsConstructor
@Component
public class Calories implements Command {

    private static final String ROOT_COMMAND = "/calories";
    private static final String ADD_CALORIES_BY_PRODUCT_ID_COMMAND = "_add_";
    private static final String ADD_PRODUCT_BY_PRODUCT_ID_COMMAND = "_add_product_";
    private static final String DELETE_PRODUCT_COMMAND = "_del_product_";
    private static final String DELETE_EATEN_PRODUCT_COMMAND = "_del_";
    private static final String DELETE_ACTIVITY_COMMAND = "_del_activity_";
    public static final int MAX_SIZE_OF_SEARCH_RESULTS = 15;
    private static final int MAX_PRODUCT_NAME_LENGTH = 255;
    private static final int MEAL_DURATION_SECONDS = 1800;
    private static final String NUMERIC_PARAMETER_TEMPLATE = "\\b(\\d+[.,]?\\d*)\\s?[%s]\\b";
    private static final String NEGATIVE_NUMERIC_PARAMETER_TEMPLATE = "-(\\d+[.,]?\\d*)\\s?[%s]\\b";
    private static final DecimalFormat DF = new DecimalFormat("#.#");
    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4})");
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile("(\\d{2})\\.(\\d{2})");

    private final InternationalizationService internationalizationService;
    private final SpeechService speechService;
    private final ProductService productService;
    private final EatenProductService eatenProductService;
    private final ActivityService activityService;
    private final UserCaloriesService userCaloriesService;
    private final UserCaloriesTargetService userCaloriesTargetService;
    private final UserCityService userCityService;
    private final CaloricMapper caloricMapper;
    private final Clock clock;

    private Pattern proteinsPattern;
    private Pattern fatsPattern;
    private Pattern carbsPattern;
    private Pattern kcalPattern;
    private Pattern gramsPattern;
    private Pattern negativeKcalPattern;

    @PostConstruct
    private void postConstruct() {
        proteinsPattern = Pattern.compile(getNumericParameterPattern("command.calories.proteinssymbol"));
        fatsPattern = Pattern.compile(getNumericParameterPattern("command.calories.fatssymbol"));
        carbsPattern = Pattern.compile(getNumericParameterPattern("command.calories.carbssymbol"));
        kcalPattern = Pattern.compile(getNumericParameterPattern("command.calories.calsymbol"));
        gramsPattern = Pattern.compile(getNumericParameterPattern("command.calories.gramssymbol"));
        negativeKcalPattern = Pattern.compile(
                String.format(
                        NEGATIVE_NUMERIC_PARAMETER_TEMPLATE,
                        String.join("", internationalizationService.getAllTranslations("command.calories.calsymbol"))));
    }

    private String getNumericParameterPattern(String code) {
        return String.format(NUMERIC_PARAMETER_TEMPLATE, String.join("", internationalizationService.getAllTranslations(code)));
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        String commandArgument = message.getCommandArgument();

        String responseText;
        if (commandArgument == null) {
            responseText = getCurrentCalories(chat, user);
        } else if (commandArgument.startsWith("_")) {
            responseText = parseCommand(chat, user, commandArgument);
        } else {
            responseText = matchCommand(chat, user, commandArgument);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private String parseCommand(Chat chat, User user, String command) {
        if (command.startsWith(ADD_PRODUCT_BY_PRODUCT_ID_COMMAND)) {
            return processAddProductByProductIdCommand(user, command);
        } else if (command.startsWith(ADD_CALORIES_BY_PRODUCT_ID_COMMAND)) {
            return processAddCaloriesByProductIdCommand(chat, user, command);
        } else if (command.startsWith(DELETE_PRODUCT_COMMAND)) {
            return deleteProduct(user, command);
        } else if (command.startsWith(DELETE_ACTIVITY_COMMAND)) {
            return deleteActivity(user, command);
        } else if (command.startsWith(DELETE_EATEN_PRODUCT_COMMAND)) {
            return deleteEatenProduct(user, command);
        }

        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
    }

    private String processAddProductByProductIdCommand(User user, String command) {
        long productId;
        try {
            productId = Long.parseLong(command.substring(ADD_PRODUCT_BY_PRODUCT_ID_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Product product = productService.get(productId);
        if (product == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return saveProduct(user, copyProduct(product, user));
    }

    private Product copyProduct(Product product, User user) {
        return new Product()
                .setUser(user)
                .setName(product.getName())
                .setProteins(product.getProteins())
                .setFats(product.getFats())
                .setCarbs(product.getCarbs())
                .setCaloric(product.getCaloric());
    }

    private String processAddCaloriesByProductIdCommand(Chat chat, User user, String command) {
        String productIdAndGrams = command.substring(ADD_CALORIES_BY_PRODUCT_ID_COMMAND.length());

        int underscoreIndex = productIdAndGrams.indexOf("_");
        if (underscoreIndex < 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        int grams;
        long productId;
        try {
            productId = Long.parseLong(productIdAndGrams.substring(0, underscoreIndex));
            grams = Integer.parseInt(productIdAndGrams.substring(underscoreIndex + 1));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Product product = productService.get(productId);
        if (product == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        LocalDateTime dateTime = getUsersCurrentDateTime(chat, user);
        userCaloriesService.addCalories(user, dateTime, product, grams);

        return buildAddedCaloriesString(caloricMapper.toCalories(product, grams)) + product.getName();
    }

    private String deleteProduct(User user, String command) {
        long productId;
        try {
            productId = Long.parseLong(command.substring(DELETE_PRODUCT_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Product product = productService.get(productId);
        if (product == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        if (!product.getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        productService.remove(product);

        return speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
    }

    private String deleteActivity(User user, String command) {
        long activityId;
        try {
            activityId = Long.parseLong(command.substring(DELETE_ACTIVITY_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Activity activity = activityService.get(activityId);
        if (activity == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        if (!activity.getUserCalories().getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        activityService.remove(activity);

        return buildAddedCaloriesString(caloricMapper.toCalories(activity));
    }

    private String deleteEatenProduct(User user, String command) {
        long eatenProductId;
        try {
            eatenProductId = Long.parseLong(command.substring(DELETE_EATEN_PRODUCT_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        EatenProduct eatenProduct = eatenProductService.get(eatenProductId);
        if (eatenProduct == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        if (!eatenProduct.getUserCalories().getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        eatenProductService.remove(eatenProduct);

        return buildSubtractCaloriesString(caloricMapper.toCalories(eatenProduct));
    }

    private String matchCommand(Chat chat, User user, String commandArgument) {
        Matcher gramsMatcher = gramsPattern.matcher(commandArgument);
        if (gramsMatcher.find()) {
            return addCaloriesByProduct(chat, user, gramsMatcher, commandArgument);
        }

        Matcher matcher = negativeKcalPattern.matcher(commandArgument);
        if (matcher.find()) {
            return saveActivity(chat, user, commandArgument, matcher);
        }

        Product product = getAddingProduct(user, commandArgument);
        if (product != null) {
            return saveProduct(user, product);
        }

        LocalDate date = searchForDate(commandArgument);
        if (date != null) {
            return getCaloriesForDate(user, date);
        }

        return getProductInfo(user, commandArgument);
    }

    private LocalDate searchForDate(String data) {
        String dateRaw = null;
        Matcher matcher = FULL_DATE_PATTERN.matcher(data);
        if (matcher.find()) {
            dateRaw = data.substring(matcher.start(), matcher.end());
        } else {
            matcher = SHORT_DATE_PATTERN.matcher(data);
            if (matcher.find()) {
                dateRaw = data.substring(matcher.start(), matcher.end()) + "." + LocalDate.now(clock).getYear();
            }
        }

        if (dateRaw == null) {
            return null;
        }

        try {
            return LocalDate.parse(dateRaw, DateUtils.dateFormatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String getCaloriesForDate(User user, LocalDate date) {
        return getCaloriesInfo(userCaloriesService.get(user, date), date);
    }

    private String getCurrentCalories(Chat chat, User user) {
        LocalDate date = getUsersCurrentDateTime(chat, user).toLocalDate();
        return getCaloriesInfo(userCaloriesService.get(user, date), date);
    }

    private String saveProduct(User user, Product product) {
        Product foundProduct = productService.get(user, product.getName());
        if (foundProduct != null) {
            foundProduct
                    .setProteins(product.getProteins())
                    .setFats(product.getFats())
                    .setCarbs(product.getCarbs())
                    .setCaloric(product.getCaloric());

            productService.save(foundProduct);

            return "${command.calories.updateproduct}:\n" + buildProductInfo(foundProduct);
        }

        productService.save(product);

        return "${command.calories.saveproduct}:\n" + buildProductInfo(product);
    }

    private String saveActivity(Chat chat, User user, String text, Matcher matcher) {
        double calories = parseValue(matcher.group(1));
        String name = text.replace(matcher.group(), "").trim();

        validateName(name);

        LocalDateTime dateTime = getUsersCurrentDateTime(chat, user);
        userCaloriesService.subtractCalories(user, dateTime, name, calories);

        return "${command.calories.saveactivity}\n" + name + ": <b>-" + DF.format(calories) + "${command.calories.calsymbol}</b>";
    }

    private Product getAddingProduct(User user, String command) {
        Matcher proteinsMatcher = proteinsPattern.matcher(command);
        Matcher fatsMatcher = fatsPattern.matcher(command);
        Matcher carbsMatcher = carbsPattern.matcher(command);
        Matcher kcalMarcher = kcalPattern.matcher(command);

        String name = command;
        double proteins = 0;
        double fats = 0;
        double carbs = 0;
        double kcal = 0;
        if (proteinsMatcher.find()) {
            proteins = parseValue(proteinsMatcher.group(1));
            name = name.replace(proteinsMatcher.group(), "");
        }
        if (fatsMatcher.find()) {
            fats = parseValue(fatsMatcher.group(1));
            name = name.replace(fatsMatcher.group(), "");
        }
        if (carbsMatcher.find()) {
            carbs = parseValue(carbsMatcher.group(1));
            name = name.replace(carbsMatcher.group(), "");
        }
        if (kcalMarcher.find()) {
            kcal = parseValue(kcalMarcher.group(1));
            name = name.replace(kcalMarcher.group(), "");
        }

        if (proteins == 0 && fats == 0 && carbs == 0 && kcal == 0) {
            return null;
        }

        if (kcal == 0) {
            kcal = caloricMapper.toCaloric(proteins, fats, carbs);
        }

        name = name.trim();
        if (!StringUtils.hasLength(name)) {
            return null;
        }

        validateName(name);

        return new Product()
                .setUser(user)
                .setName(name)
                .setProteins(proteins)
                .setFats(fats)
                .setCarbs(carbs)
                .setCaloric(kcal);
    }

    private void validateName(String name) {
        if (name.length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private String addCaloriesByProduct(Chat chat, User user, Matcher matcher, String command) {
        double grams = parseValue(matcher.group(1));

        if (grams == 0D) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String name = command.replace(matcher.group(), "").trim();
        if (!StringUtils.hasLength(name)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Product product = productService.get(user, name);
        if (product == null) {
            Collection<Product> foundProducts = productService.find(user, name, MAX_SIZE_OF_SEARCH_RESULTS);
            if (foundProducts.size() == 1) {
                product = foundProducts.iterator().next();
            } else {
                int intGrams = (int) grams;
                String foundProductsInfo = foundProducts
                        .stream()
                        .map(foundProduct -> buildFoundToAddCaloriesProduct(foundProduct, intGrams))
                        .collect(Collectors.joining("\n"));
                return "${command.calories.unknownproduct}: <b>" + name + "</b>\n\n" + foundProductsInfo;
            }
        }

        LocalDateTime dateTime = getUsersCurrentDateTime(chat, user);
        userCaloriesService.addCalories(user, dateTime, product, grams);

        return buildAddedCaloriesString(caloricMapper.toCalories(product, grams)) + product.getName();
    }

    private String buildFoundToAddCaloriesProduct(Product product, int grams) {
        double caloric = product.getCaloric() / 100 * grams;
        return buildProductInfo(product) + "\n " + ROOT_COMMAND + ADD_CALORIES_BY_PRODUCT_ID_COMMAND + product.getId() + "_" + grams + " <b>+" + DF.format(caloric) + "</b> ${command.calories.kcal}\n";
    }

    private String getProductInfo(User user, String name) {
        Page<Product> products = productService.find(name, MAX_SIZE_OF_SEARCH_RESULTS);

        if (products.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        Long userId = user.getUserId();
        String foundProducts = products
                .stream()
                .limit(MAX_SIZE_OF_SEARCH_RESULTS)
                .map(product -> buildFoundProductInfo(product, userId))
                .collect(Collectors.joining("\n"));

        return foundProducts + "\n${command.calories.totalproductsfound}: <b>" + products.getTotalElements() + "</b>";
    }

    private String buildFoundProductInfo(Product product, Long userId) {
        String productInfo = buildProductInfo(product);

        if (!product.getUser().getUserId().equals(userId)) {
            return productInfo + "\n " + ROOT_COMMAND + ADD_PRODUCT_BY_PRODUCT_ID_COMMAND + product.getId() + "\n";
        } else {
            return productInfo + "\n " + ROOT_COMMAND + DELETE_PRODUCT_COMMAND + product.getId() + "\n";
        }
    }

    private String buildProductInfo(Product product) {
        return product.getName() + " " + getPFCInfo(product);
    }

    private String buildSubtractCaloriesString(org.telegram.bot.domain.Calories calories) {
        return buildChangedCaloriesString("${command.calories.deleted}", calories.getProteins(), calories.getFats(), calories.getCarbs(), calories.getCaloric());
    }

    private String buildAddedCaloriesString(org.telegram.bot.domain.Calories calories) {
        return buildChangedCaloriesString("${command.calories.added}", calories.getProteins(), calories.getFats(), calories.getCarbs(), calories.getCaloric());
    }

    private String buildChangedCaloriesString(String caption, double proteins, double fats, double carbs, double caloric) {
        String PFC;
        if (proteins == 0D && fats == 0D && carbs == 0D) {
            PFC = "";
        } else {
            String proteinsString = "";
            if (proteins != 0D) {
                proteinsString = "<b>" + DF.format(proteins) + "</b> ${command.calories.proteinssymbol}. ";
            }

            String fatsString = "";
            if (fats != 0D) {
                fatsString = "<b>" + DF.format(fats) + "</b> ${command.calories.fatssymbol}. ";
            }

            String carbsString = "";
            if (carbs != 0D) {
                carbsString = "<b>" + DF.format(carbs) + "</b> ${command.calories.carbssymbol}. ";
            }

            PFC = "(" + proteinsString + fatsString + carbsString + ")";
        }

        return caption + ": <b>" + DF.format(caloric) + "</b> ${command.calories.kcal}.\n" + PFC;
    }

    private String getCaloriesInfo(UserCalories userCalories, LocalDate date) {
        Set<EatenProduct> eatenProducts = userCalories.getEatenProducts();
        Map<EatenProduct, org.telegram.bot.domain.Calories> eatenProductCaloriesMap;
        if (eatenProducts == null) {
            eatenProductCaloriesMap = Map.of();
        } else {
            eatenProductCaloriesMap = userCalories.getEatenProducts()
                    .stream()
                    .sorted(Comparator.comparing(EatenProduct::getDateTime))
                    .collect(Collectors.toMap(
                            eatenProduct -> eatenProduct,
                            caloricMapper::toCalories,
                            (existing, replacement) -> existing,
                            LinkedHashMap::new));
        }

        org.telegram.bot.domain.Calories calories = caloricMapper.sum(eatenProductCaloriesMap.values()); // product parameters may change, so it is better to recalculate each time

        Set<Activity> activities = userCalories.getActivities();
        double caloriesBurned = activities.stream().mapToDouble(Activity::getCalories).sum();

        String caloriesOfTargetInfo = "";
        String proteinsOfTargetInfo = "";
        String fatsOfTargetInfo = "";
        String carbsOfTargetInfo = "";
        UserCaloriesTarget userCaloriesTarget = userCaloriesTargetService.get(userCalories.getUser());
        if (userCaloriesTarget != null) {
            Double caloriesTarget = userCaloriesTarget.getCalories();
            if (caloriesTarget != null) {
                String leftItem;
                double caloriesLeft = userCaloriesTarget.getCalories() - calories.getCaloric() + caloriesBurned;
                if (caloriesLeft < 0) {
                    leftItem = "${command.calories.noneleft}";
                    caloriesLeft = Math.abs(caloriesLeft);
                } else {
                    leftItem = "${command.calories.left}";
                }

                caloriesOfTargetInfo = "(" + DF.format(getPercent(caloriesTarget, calories.getCaloric())) + "%)\n"
                        + leftItem + ": <b>" + DF.format(caloriesLeft) + "</b> ${command.calories.kcal}\n";
            }

            if (userCaloriesTarget.getProteins() != null) {
                proteinsOfTargetInfo = "(" + DF.format(getPercent(userCaloriesTarget.getProteins(), calories.getProteins())) + "%)";
            }
            if (userCaloriesTarget.getFats() != null) {
                fatsOfTargetInfo = "(" + DF.format(getPercent(userCaloriesTarget.getFats(), calories.getFats())) + "%)";
            }
            if (userCaloriesTarget.getCarbs() != null) {
                carbsOfTargetInfo = "(" + DF.format(getPercent(userCaloriesTarget.getCarbs(), calories.getCarbs())) + "%)";
            }
        }

        String caloriesOfActivitiesInfo;
        if (activities.isEmpty()) {
            caloriesOfActivitiesInfo = "";
        } else {
            caloriesOfActivitiesInfo = "${command.calories.burned}: <b>" + DF.format(caloriesBurned) + "</b> ${command.calories.kcal}.\n";
        }

        Map<LocalDateTime, String> eatenProductListInfo = getEatenProductListInfo(eatenProductCaloriesMap, userCaloriesTarget);
        Map<LocalDateTime, String> activitiesInfo = getActivitiesInfo(activities);
        Map<LocalDateTime, String> dayDetails = new HashMap<>(eatenProductListInfo.size() + activitiesInfo.size());
        dayDetails.putAll(eatenProductListInfo);
        dayDetails.putAll(activitiesInfo);

        return "<b><u>${command.calories.caption} " + DateUtils.formatDate(date) + ":</u></b>\n"
                + caloriesOfActivitiesInfo
                + "${command.calories.eaten}: <b>" + DF.format(calories.getCaloric()) + "</b> ${command.calories.kcal}. "
                + caloriesOfTargetInfo
                + "\n<b><u>${command.calories.caption2}:</u></b>\n"
                + "${command.calories.proteins}: <b>" + DF.format(calories.getProteins()) + "</b> ${command.calories.gramssymbol}. "
                + proteinsOfTargetInfo + "\n"
                + "${command.calories.fats}: <b>" + DF.format(calories.getFats()) + "</b> ${command.calories.gramssymbol}. "
                + fatsOfTargetInfo + "\n"
                + "${command.calories.carbs}: <b>" + DF.format(calories.getCarbs()) + "</b> ${command.calories.gramssymbol}. "
                + carbsOfTargetInfo + "\n\n"
                + buildDetailsInfo(dayDetails);
    }

    private double getPercent(double dividend, double divisor) {
        return divisor / dividend * 100;
    }

    private String buildDetailsInfo(Map<LocalDateTime, String> details) {
        return details.entrySet()
                .stream()
                .sorted(((Map.Entry.comparingByKey())))
                .map(Map.Entry::getValue).collect(Collectors.joining("\n"));
    }

    private Map<LocalDateTime, String> getEatenProductListInfo(Map<EatenProduct, org.telegram.bot.domain.Calories> eatenProductCaloriesMap, UserCaloriesTarget userCaloriesTarget) {
        Map<LocalDateTime, String> result = new HashMap<>();
        StringBuilder mealBuf = new StringBuilder();

        LocalDateTime startMealDateTime = null;
        LocalDateTime stopMealDateTime = null;
        org.telegram.bot.domain.Calories mealCalories = new org.telegram.bot.domain.Calories();
        for (Map.Entry<EatenProduct, org.telegram.bot.domain.Calories> entry : eatenProductCaloriesMap.entrySet()) {
            EatenProduct eatenProduct = entry.getKey();
            org.telegram.bot.domain.Calories calories = entry.getValue();

            if (startMealDateTime != null) {
                Duration mealDuration = Duration.between(startMealDateTime, eatenProduct.getDateTime());
                if (mealDuration.getSeconds() > MEAL_DURATION_SECONDS) {
                    result.put(stopMealDateTime, buildTimeCutoff(startMealDateTime, stopMealDateTime, mealCalories, userCaloriesTarget) + BORDER + mealBuf);

                    mealBuf = new StringBuilder();
                    mealCalories = new org.telegram.bot.domain.Calories();
                    startMealDateTime = eatenProduct.getDateTime();
                }
            } else {
                startMealDateTime = eatenProduct.getDateTime();
            }

            mealCalories.addCalories(calories);
            stopMealDateTime = eatenProduct.getDateTime();
            mealBuf.append("<b>•</b> ").append(getEatenProductInfo(eatenProduct, calories)).append("\n");
        }

        if (startMealDateTime != null && stopMealDateTime != null) {
            result.put(stopMealDateTime, buildTimeCutoff(startMealDateTime, stopMealDateTime, mealCalories, userCaloriesTarget) + BORDER + mealBuf);
        }

        return result;
    }

    private Map<LocalDateTime, String> getActivitiesInfo(Set<Activity> activities) {
        if (activities == null) {
            return Map.of();
        }

        return activities
                .stream()
                .collect(Collectors.toMap(
                        Activity::getDateTime,
                        activity -> getActivityInfo(activity) + "\n"));
    }

    private String buildTimeCutoff(LocalDateTime from, LocalDateTime to, org.telegram.bot.domain.Calories mealCalories, UserCaloriesTarget caloriesTarget) {
        return buildTimeCutoff(from.toLocalTime(), to.toLocalTime(), mealCalories, caloriesTarget);
    }

    private String buildTimeCutoff(LocalTime from, LocalTime to, org.telegram.bot.domain.Calories mealCalories, UserCaloriesTarget caloriesTarget) {
        String timeCutoff;
        if (from.equals(to)) {
            timeCutoff = DateUtils.formatShortTime(from);
        } else {
            timeCutoff = DateUtils.formatShortTime(from) + " — " + DateUtils.formatShortTime(to);
        }

        Double caloricTarget = Optional.ofNullable(caloriesTarget).map(UserCaloriesTarget::getCalories).orElse(null);
        double mealCaloric = mealCalories.getCaloric();
        StringBuilder caloricInfo = new StringBuilder();
        if (mealCaloric != 0D) {
            caloricInfo.append("<b>").append(DF.format(mealCaloric)).append(" ${command.calories.kcal}.</b> ");
            if (caloricTarget != null) {
                caloricInfo.append("(").append(DF.format(getPercent(caloricTarget, mealCaloric))).append("%) ");
            }
            caloricInfo.append("\n");
        }

        return "<u><b>" + timeCutoff + "</b></u>: " + caloricInfo + getMealCaloricInfo(mealCalories, caloriesTarget);
    }

    private String getMealCaloricInfo(org.telegram.bot.domain.Calories mealCalories, UserCaloriesTarget caloriesTarget) {
        Double targetProteins = null;
        Double targetFats = null;
        Double targetCarbs = null;

        if (caloriesTarget != null) {
            targetProteins = caloriesTarget.getProteins();
            targetFats = caloriesTarget.getFats();
            targetCarbs = caloriesTarget.getCarbs();
        }

        return getMealCaloricParamInfo(mealCalories.getProteins(), targetProteins, "${command.calories.proteins}")
                + getMealCaloricParamInfo(mealCalories.getFats(), targetFats, "${command.calories.fats}")
                + getMealCaloricParamInfo(mealCalories.getCarbs(), targetCarbs, "${command.calories.carbs}");
    }

    private String getMealCaloricParamInfo(double mealParam, Double targetParam, String caption) {
        StringBuilder buf = new StringBuilder();
        if (mealParam != 0D) {
            buf.append(caption).append(": <b>").append(DF.format(mealParam)).append("</b> ${command.calories.gramssymbol}. ");
            if (targetParam != null) {
                buf.append("(").append(DF.format(getPercent(targetParam, mealParam))).append("%) ");
            }
            buf.append("\n");
        }

        return buf.toString();
    }

    private String getEatenProductInfo(EatenProduct eatenProduct, org.telegram.bot.domain.Calories calories) {
        Product product = eatenProduct.getProduct();
        return product.getName() + " (" + DF.format(eatenProduct.getGrams()) + " ${command.calories.gramssymbol}.) — "
                + getPFCInfo(calories) + "\n"
                + " " + ROOT_COMMAND + DELETE_EATEN_PRODUCT_COMMAND + eatenProduct.getId();
    }

    private String getActivityInfo(Activity activity) {
        return "<u><b>" + DateUtils.formatShortTime(activity.getDateTime().toLocalTime()) + "</b></u>:\n"
                + "<b>•</b> " + activity.getName() + " — <b>-" + DF.format(activity.getCalories()) + "</b> ${command.calories.kcal}.\n"
                + " " + ROOT_COMMAND + DELETE_ACTIVITY_COMMAND + activity.getId();
    }

    private String getPFCInfo(Product product) {
        return getPFCInfo(product.getCaloric(), product.getProteins(), product.getFats(), product.getCarbs());
    }

    private String getPFCInfo(org.telegram.bot.domain.Calories calories) {
        return getPFCInfo(calories.getCaloric(), calories.getProteins(), calories.getFats(), calories.getCarbs());
    }

    private String getPFCInfo(double caloric, double proteins, double fats, double carbs) {
        return "<b>" + DF.format(caloric) + "</b> ${command.calories.kcal}.\n"
                + "${command.calories.proteinssymbol}: <b>" + DF.format(proteins) + "</b> ${command.calories.gramssymbol}. "
                + "${command.calories.fatssymbol}: <b>" + DF.format(fats) + "</b>${command.calories.gramssymbol}. "
                + "${command.calories.carbssymbol}: <b>" + DF.format(carbs) + "</b>${command.calories.gramssymbol}.";
    }

    private LocalDateTime getUsersCurrentDateTime(Chat chat, User user) {
        ZoneId zoneIdOfUser = userCityService.getZoneIdOfUserOrDefault(chat, user);
        return LocalDateTime.now(clock.withZone(zoneIdOfUser));
    }

    private double parseValue(String data) {
        data = data.replace(",", ".");

        try {
            return Double.parseDouble(data);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

}
