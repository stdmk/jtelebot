package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.mapper.caloric.CaloricMapper;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.calories.EatenProductService;
import org.telegram.bot.services.calories.ProductService;
import org.telegram.bot.services.calories.UserCaloriesService;
import org.telegram.bot.services.calories.UserCaloriesTargetService;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class Calories implements Command {

    private static final String ROOT_COMMAND = "/calories";
    private static final String ADD_CALORIES_BY_PRODUCT_ID_COMMAND = "_add_";
    private static final String ADD_PRODUCT_BY_PRODUCT_ID_COMMAND = "_add_product_";
    private static final String DELETE_PRODUCT_COMMAND = "_del_product_";
    private static final String DELETE_EATEN_PRODUCT_COMMAND = "_del_";
    private static final int MAX_SIZE_OF_SEARCH_RESULTS = 30;
    private static final int MAX_PRODUCT_NAME_LENGTH = 255;
    private static final String NUMERIC_PARAMETER_TEMPLATE = "\\b(\\d+[.,]?\\d*)\\s?[%s]\\b";
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    private final InternationalizationService internationalizationService;
    private final SpeechService speechService;
    private final ProductService productService;
    private final EatenProductService eatenProductService;
    private final UserCaloriesService userCaloriesService;
    private final UserCaloriesTargetService userCaloriesTargetService;
    private final UserCityService userCityService;
    private final CaloricMapper caloricMapper;

    private Pattern proteinsPattern;
    private Pattern fatsPattern;
    private Pattern carbsPattern;
    private Pattern kcalPattern;
    private Pattern gramsPattern;

    @PostConstruct
    private void postConstruct() {
        proteinsPattern = Pattern.compile(getNumericParameterPattern("command.calories.proteinssymbol"));
        fatsPattern = Pattern.compile(getNumericParameterPattern("command.calories.fatssymbol"));
        carbsPattern = Pattern.compile(getNumericParameterPattern("command.calories.carbssymbol"));
        kcalPattern = Pattern.compile(getNumericParameterPattern("command.calories.calsymbol"));
        gramsPattern = Pattern.compile(getNumericParameterPattern("command.calories.gramssymbol"));
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
            return deleteProduct(chat, user, command);
        } else if (command.startsWith(DELETE_EATEN_PRODUCT_COMMAND)) {
            return deleteEatenProduct(chat, user, command);
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

        return addCaloriesByProduct(chat, user, product, grams);
    }

    private String deleteProduct(Chat chat, User user, String command) {
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

    private String deleteEatenProduct(Chat chat, User user, String command) {
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

        return getCurrentCalories(chat, user);
    }

    private String matchCommand(Chat chat, User user, String commandArgument) {
        Matcher gramsMatcher = gramsPattern.matcher(commandArgument);
        if (gramsMatcher.find()) {
            return addCaloriesByProduct(chat, user, gramsMatcher, commandArgument);
        }

        Product product = getAddingProduct(user, commandArgument);
        if (product != null) {
            return saveProduct(user, product);
        }

        return getProductInfo(user, commandArgument);
    }

    private String getCurrentCalories(Chat chat, User user) {
        ZoneId zoneIdOfUser = userCityService.getZoneIdOfUserOrDefault(chat, user);
        return getCurrentCalories(userCaloriesService.get(user, zoneIdOfUser));
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

        validateProductName(name);

        return new Product()
                .setUser(user)
                .setName(name)
                .setProteins(proteins)
                .setFats(fats)
                .setCarbs(carbs)
                .setCaloric(kcal);
    }

    private void validateProductName(String name) {
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
            int intGrams = (int) grams;
            String foundProducts = productService.find(user, name, MAX_SIZE_OF_SEARCH_RESULTS)
                    .stream()
                    .map(foundProduct -> buildFoundToAddCaloriesProduct(foundProduct, intGrams))
                    .collect(Collectors.joining("\n"));
            return "${command.calories.unknownproduct}: <b>" + name + "</b>\n\n" + foundProducts;
        }

        return addCaloriesByProduct(chat, user, product, grams);
    }

    private String addCaloriesByProduct(Chat chat, User user, Product product, double grams) {
        ZoneId zoneIdOfUser = userCityService.getZoneIdOfUserOrDefault(chat, user);
        UserCalories userCalories = userCaloriesService.addCalories(user, zoneIdOfUser, product, grams);
        return buildAddedCaloriesString(caloricMapper.toCalories(product, grams)) + "\n\n" + getCurrentCalories(userCalories);
    }

    private String buildFoundToAddCaloriesProduct(Product product, int grams) {
        return buildProductInfo(product) + " " + ROOT_COMMAND + ADD_CALORIES_BY_PRODUCT_ID_COMMAND + product.getId() + "_" + grams;
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
            return productInfo + " " + ROOT_COMMAND + ADD_PRODUCT_BY_PRODUCT_ID_COMMAND + product.getId();
        } else {
            return productInfo + " " + ROOT_COMMAND + DELETE_PRODUCT_COMMAND + product.getId();
        }
    }

    private String buildProductInfo(Product product) {
        return product.getName() + " <b>" + DF.format(product.getCaloric()) + "</b> ${command.calories.kcal} "
                + "(<b>" + DF.format(product.getProteins()) + "</b> ${command.calories.proteinssymbol}. <b>"
                + DF.format(product.getFats()) + "</b> ${command.calories.fatssymbol}. <b>"
                + DF.format(product.getCarbs()) + "</b> ${command.calories.carbssymbol}.)";
    }

    private String buildAddedCaloriesString(org.telegram.bot.domain.Calories calories) {
        return buildAddedCaloriesString(calories.getProteins(), calories.getFats(), calories.getCarbs(), calories.getCaloric());
    }

    private String buildAddedCaloriesString(double proteins, double fats, double carbs, double caloric) {
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

        return "${command.calories.added}: <b>" + DF.format(caloric) + "</b> ${command.calories.kcal}.\n("
                + proteinsString + fatsString + carbsString + ")";
    }

    private String getCurrentCalories(UserCalories userCalories) {
        Map<EatenProduct, org.telegram.bot.domain.Calories> eatenProductCaloriesMap = userCalories.getEatenProducts()
                .stream()
                .collect(Collectors.toMap(
                        eatenProduct -> eatenProduct,
                        caloricMapper::toCalories));

        org.telegram.bot.domain.Calories calories = caloricMapper.sum(eatenProductCaloriesMap.values()); // product parameters may change, so it is better to recalculate each time

        String caloriesOfTargetInfo = "";
        String proteinsOfTargetInfo = "";
        String fatsOfTargetInfo = "";
        String carbsOfTargetInfo = "";
        UserCaloriesTarget userCaloriesTarget = userCaloriesTargetService.get(userCalories.getUser());
        if (userCaloriesTarget != null) {
            Double caloriesTarget = userCaloriesTarget.getCalories();
            if (caloriesTarget != null) {
                String leftItem;
                double caloriesLeft = userCaloriesTarget.getCalories() - calories.getCaloric();
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

        return "<b><u>${command.calories.caption}:</u></b>\n"
                + "${command.calories.eaten}: <b>" + DF.format(calories.getCaloric()) + "</b> ${command.calories.kcal}. "
                + caloriesOfTargetInfo
                + "\n<b><u>${command.calories.caption2}:</u></b>\n"
                + "${command.calories.proteins}: <b>" + DF.format(calories.getProteins()) + "</b> ${command.calories.gramssymbol}. "
                + proteinsOfTargetInfo + "\n"
                + "${command.calories.fats}: <b>" + DF.format(calories.getFats()) + "</b> ${command.calories.gramssymbol}. "
                + fatsOfTargetInfo + "\n"
                + "${command.calories.carbs}: <b>" + DF.format(calories.getCarbs()) + "</b> ${command.calories.gramssymbol}. "
                + carbsOfTargetInfo + "\n"
                + "\n<b><u>${command.calories.caption3}:</u></b>\n"
                + getEatenProductListInfo(eatenProductCaloriesMap);
    }

    private double getPercent(double dividend, double divisor) {
        return divisor / dividend * 100;
    }

    private String getEatenProductListInfo(Map<EatenProduct, org.telegram.bot.domain.Calories> eatenProductCaloriesMap) {
        return eatenProductCaloriesMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(eatenProductCaloriesEntry -> eatenProductCaloriesEntry.getKey().getDateTime()))
                .map(this::getEatenProductInfo)
                .collect(Collectors.joining("\n"));
    }

    private String getEatenProductInfo(Map.Entry<EatenProduct, org.telegram.bot.domain.Calories> eatenProductCaloriesEntry) {
        return getEatenProductInfo(eatenProductCaloriesEntry.getKey(), eatenProductCaloriesEntry.getValue());
    }

    private String getEatenProductInfo(EatenProduct eatenProduct, org.telegram.bot.domain.Calories calories) {
        Product product = eatenProduct.getProduct();
        return product.getName() + " (" + DF.format(eatenProduct.getGrams()) + " ${command.calories.gramssymbol}.) — <b>" + DF.format(calories.getCaloric()) + " ${command.calories.kcal}.</b>\n"
                + " " + ROOT_COMMAND + DELETE_EATEN_PRODUCT_COMMAND + eatenProduct.getId();
    }

    private Double parseValue(String data) {
        data = data.replace(",", ".");

        try {
            return Double.parseDouble(data);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

}
