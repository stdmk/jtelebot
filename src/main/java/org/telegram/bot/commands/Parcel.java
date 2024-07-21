package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.timers.TrackCodeEventsTimer;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatDateTime;
import static org.telegram.bot.utils.DateUtils.formatShortDateTime;
import static org.telegram.bot.utils.TextUtils.BORDER;

@Component
@RequiredArgsConstructor
public class Parcel implements Command {

    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings().setFormattingStyle(FormattingStyle.HTML);

    private final ParcelService parcelService;
    private final TrackCodeService trackCodeService;
    private final UserService userService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final Bot bot;
    private final BotStats botStats;
    private final PropertiesConfig propertiesConfig;
    private final Clock clock;

    private static final String EMPTY_COMMAND = "parcel";
    private static final String CALLBACK_COMMAND = EMPTY_COMMAND + " ";
    private static final String DELETE_PARCEL_COMMAND = "удалить";
    private static final String SHORT_DELETE_PARCEL_COMMAND = "_d";
    private static final String CALLBACK_DELETE_PARCEL_COMMAND = CALLBACK_COMMAND + DELETE_PARCEL_COMMAND;
    private static final String ADD_PARCEL_COMMAND = "добавить";
    private static final String CALLBACK_ADD_PARCEL_COMMAND = CALLBACK_COMMAND + ADD_PARCEL_COMMAND;
    public static final String DELIVERED_OPERATION_TYPE = "Вручение";
    private static final String TRACKING_ON_SITE_URL = "https://www.pochta.ru/tracking?barcode=";

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        Chat chat = message.getChat();
        User user = message.getUser();

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        String commandArgument;
        if (commandWaiting == null) {
            commandArgument = message.getCommandArgument();
        } else {
            String text = message.getText();
            if (text == null) {
                text = "";
            }
            commandArgument = TextUtils.cutCommandInText(commandWaiting.getTextMessage()) + text;
        }

        if (message.isCallback()) {
            if (commandArgument == null || commandArgument.isEmpty()) {
                return returnResponse(getMainMenu(message, user, false));
            } else if (commandArgument.startsWith(DELETE_PARCEL_COMMAND)) {
                return returnResponse(deleteParcelByCallback(message, user, commandArgument));
            } else if (commandArgument.startsWith(ADD_PARCEL_COMMAND)) {
                return returnResponse(addParcelByCallback(message, chat, user, commandArgument));
            }
        }

        if (commandArgument == null || commandArgument.equals(EMPTY_COMMAND)) {
            return returnResponse(getMainMenu(message,  user, true));
        } else if (commandArgument.startsWith(ADD_PARCEL_COMMAND)) {
            return returnResponse(addParcel(message, user, commandArgument, commandWaiting));
        } else if (commandArgument.startsWith(DELETE_PARCEL_COMMAND) || (commandArgument.startsWith(SHORT_DELETE_PARCEL_COMMAND))) {
            return returnResponse(deleteParcel(message, user, commandArgument));
        } else {
            return getTrackCodeData(message, user, commandArgument);
        }
    }

    private BotResponse getMainMenu(Message message, User user, boolean newMessage) throws BotException {
        List<org.telegram.bot.domain.entities.Parcel> parcelList = parcelService.get(user);

        StringBuilder buf = new StringBuilder("<b>${command.parcel.caption}:</b>\n");
        parcelList.forEach(parcel -> {
            TrackCode trackCode = parcel.getTrackCode();
            String barcode = trackCode.getBarcode();
            Optional<TrackCodeEvent> optionalLastTrackCodeEvent = trackCode.getEvents()
                    .stream()
                    .max(Comparator.comparing(TrackCodeEvent::getEventDateTime));
            buf.append("<code>").append(barcode).append("</code> — <b>")
                    .append("<a href='").append(TRACKING_ON_SITE_URL).append(barcode).append("'>").append(parcel.getName()).append("</a></b>\n");

            if (optionalLastTrackCodeEvent.isPresent()) {
                buf.append(buildStringEventMessage(optionalLastTrackCodeEvent.get(), parcel.getId())).append(BORDER);
            } else {
                buf.append("${command.parcel.noinformation}\n").append(BORDER);
            }

        });

        buf.append(buildStringUpdateTimesInformation());

        List<List<KeyboardButton>> rows = parcelList.stream().map(parcel -> {
            String parcelName = Emoji.DELETE.getSymbol() + parcel.getName();
            if (parcelName.length() > 30) {
                parcelName = parcelName.substring(0, 30) + "...";
            }
            return List.of(new KeyboardButton()
                    .setName(parcelName)
                    .setCallback(CALLBACK_DELETE_PARCEL_COMMAND + parcel.getId()));
        }).collect(Collectors.toList());

        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.NEW.getSymbol() + "${command.parcel.button.add}")
                .setCallback(CALLBACK_ADD_PARCEL_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.UPDATE.getSymbol() + "${command.parcel.button.reload}")
                .setCallback(CALLBACK_COMMAND)));

        Keyboard keyboard = new Keyboard(rows);

        if (newMessage) {
            return new TextResponse()
                    .setChatId(message.getChatId())
                    .setText(buf.toString())
                    .setKeyboard(keyboard)
                    .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
        }

        return new EditResponse(message)
                .setText(buf.toString())
                .setKeyboard(keyboard)
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private EditResponse deleteParcelByCallback(Message message, User user, String textCommand) throws BotException {
        long parcelId;
        try {
            parcelId = Long.parseLong(textCommand.substring(DELETE_PARCEL_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        org.telegram.bot.domain.entities.Parcel parcel = parcelService.get(user, parcelId);
        if (parcel == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        if (!user.getUserId().equals(parcel.getUser().getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        parcelService.remove(parcel);

        return (EditResponse) getMainMenu(message, user, false);
    }

    private TextResponse addParcelByCallback(Message message, Chat chat, User user, String textCommand) {
        commandWaitingService.add(chat, user, Parcel.class, CALLBACK_COMMAND + textCommand);
        return new TextResponse(message)
                .setText("${command.parcel.parceladdinghint}: <code>${command.parcel.parceladdingexample}</code>")
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private BotResponse addParcel(Message message, User user, String textCommand, CommandWaiting commandWaiting) throws BotException {
        commandWaitingService.remove(commandWaiting);

        checkFreeTrackCodeSlots(trackCodeService.getTrackCodesCount());

        textCommand = textCommand.substring(ADD_PARCEL_COMMAND.length() + 1);

        org.telegram.bot.domain.entities.Parcel parcel;
        String barcode;
        String parcelName;

        int spaceIndex = textCommand.indexOf(" ");
        if (spaceIndex < 0) {
            barcode = textCommand;
            parcelName = textCommand;
            parcel = parcelService.getByBarcode(user, textCommand);
        } else {
            parcelName = textCommand.substring(0, spaceIndex);
            barcode = textCommand.substring(spaceIndex + 1);
            parcel = parcelService.getByBarcodeOrName(user, parcelName, barcode);
        }

        if (parcel != null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY));
        }

        TrackCode trackCode = trackCodeService.get(barcode);
        if (trackCode == null) {
            trackCode = trackCodeService.save(new TrackCode().setBarcode(barcode));
        }

        parcelService.save(new org.telegram.bot.domain.entities.Parcel()
                .setUser(user)
                .setName(parcelName)
                .setTrackCode(trackCode));

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private void checkFreeTrackCodeSlots(long occupiedSlots) {
        final int requestsLimit = propertiesConfig.getRussianPostRequestsLimit();

        long availableSlots = requestsLimit / (24 / TrackCodeEventsTimer.FIXED_RATE_HOURS);

        if (occupiedSlots >= availableSlots) {
            throw new BotException("${command.parcel.noslots}");
        }
    }

    private TextResponse deleteParcel(Message message, User user, String command) throws BotException {
        org.telegram.bot.domain.entities.Parcel parcel;

        if (command.startsWith(SHORT_DELETE_PARCEL_COMMAND)) {
            long parcelId;
            try {
                parcelId = Long.parseLong(command.substring(SHORT_DELETE_PARCEL_COMMAND.length()));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            parcel = parcelService.get(user, parcelId);
            if (parcel == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            String params;
            try {
                params = command.substring(DELETE_PARCEL_COMMAND.length() + 1);
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            if (params.isEmpty()) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            parcel = parcelService.getByName(user, params);
            if (parcel == null) {
                parcel = parcelService.getByBarcode(user, params);
                if (parcel == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            }
        }

        parcelService.remove(parcel);

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS);
    }

    private List<BotResponse> getTrackCodeData(Message message, User user, String command) {
        Long parcelId = null;
        try {
            parcelId = Long.parseLong(command.substring(1));
        } catch (NumberFormatException ignored) {
            // it means it's not a number
        }

        org.telegram.bot.domain.entities.Parcel parcel;
        if (parcelId != null) {
            parcel = parcelService.get(user, parcelId);
        } else {
            parcel = parcelService.getByBarcodeOrName(user, command);
        }

        if (parcel == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        final boolean economyMode = isEconomyMode(propertiesConfig.getRussianPostRequestsLimit());
        String lastUpdatesTimeInfo;
        TrackCode trackCode = parcel.getTrackCode();
        if (!economyMode || AccessLevel.ADMIN.getValue().equals(userService.getUserAccessLevel(user.getUserId()))) {
            LocalDateTime lastEventUpdateDateTime = trackCode.getEvents()
                    .stream()
                    .map(TrackCodeEvent::getEventDateTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now(clock));

            trackCodeService.updateFromApi(trackCode);

            notifyOtherUsers(trackCode, user, lastEventUpdateDateTime);
            lastUpdatesTimeInfo = buildStringUpdateTimesInformation(Instant.now(clock));
        } else {
            lastUpdatesTimeInfo = buildStringUpdateTimesInformation();
        }

        List<TrackCodeEvent> trackCodeEventList = new ArrayList<>(trackCode.getEvents())
                .stream()
                .sorted(Comparator.comparing(TrackCodeEvent::getEventDateTime))
                .toList();

        List<String> response = trackCodeEventList
                .stream()
                .map(trackCodeEvent -> buildStringEventMessage(trackCodeEvent) + BORDER)
                .collect(Collectors.toList());

        response.add(buildGeneralInformation(trackCode.getBarcode(), trackCodeEventList));
        response.add(lastUpdatesTimeInfo);

        return mapToTextResponseList(response, message, DEFAULT_RESPONSE_SETTINGS);
    }

    private void notifyOtherUsers(TrackCode trackCode, User user, LocalDateTime lastEventUpdateDateTime) {
        List<org.telegram.bot.domain.entities.Parcel> parcelsOfTrackCode = parcelService.getAll(trackCode)
                .stream()
                .filter(parcel -> !user.getUserId().equals(parcel.getUser().getUserId()))
                .toList();

        if (parcelsOfTrackCode.isEmpty()) {
            return;
        }

        trackCode.getEvents()
                .stream()
                .filter(event -> event.getEventDateTime().isAfter(lastEventUpdateDateTime))
                .flatMap(newEvent -> parcelsOfTrackCode.stream().map(parcel ->
                        new TextResponse()
                                .setChatId(parcel.getUser().getUserId())
                                .setText(Parcel.buildStringEventMessage(parcel, newEvent))
                                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS)))
                .forEach(bot::sendMessage);
    }

    public static String buildStringEventMessage(org.telegram.bot.domain.entities.Parcel parcel, TrackCodeEvent trackCodeEvent) {
        return "<b>" + parcel.getName() + "</b>\n" +
                "<code>" + parcel.getTrackCode().getBarcode() + "</code>\n" +
                buildStringEventMessage(trackCodeEvent, parcel.getId());
    }

    public static String buildStringEventMessage(TrackCodeEvent event) {
        return buildStringEventMessage(event, null);
    }

    public static String buildStringEventMessage(TrackCodeEvent event, Long parcelId) {
        StringBuilder buf = new StringBuilder();

        if (event.getItemName() != null) buf.append("<u>").append(event.getItemName()).append("</u>");
        if (event.getGram() != null) buf.append(" ").append(event.getGram()).append(" г.\n"); else buf.append("\n");
        if (event.getEventDateTime() != null) buf.append("<i>").append(formatDateTime(event.getEventDateTime())).append("</i>\n");
        if (event.getOperationType() != null) buf.append("<b>").append(event.getOperationType()).append("</b>");
        if (event.getOperationDescription() != null) buf.append(" ").append(event.getOperationDescription()).append("\n"); else buf.append("\n");
        if (event.getAddress() != null) buf.append(event.getAddress());
        if (event.getIndex() != null) buf.append(" (").append(event.getIndex()).append(")\n"); else buf.append("\n");

        if (parcelId != null) buf.append("/" + EMPTY_COMMAND + "_").append(parcelId).append("\n");

        return buf.toString();
    }

    private String buildGeneralInformation(String barcode, List<TrackCodeEvent> trackCodeEventList) {
        StringBuilder buf = new StringBuilder();
        buf.append("<code>").append(barcode).append("</code>\n");

        trackCodeEventList
                .stream()
                .map(TrackCodeEvent::getEventDateTime)
                .min(LocalDateTime::compareTo)
                .ifPresent(firstDateTime -> {
                    buf.append("${command.parcel.trackinfo.register}: \n").append("<b>").append(formatDateTime(firstDateTime)).append("</b>\n");

                    LocalDateTime lastDateTime = trackCodeEventList
                            .stream()
                            .filter(trackCodeEvent -> DELIVERED_OPERATION_TYPE.equalsIgnoreCase(trackCodeEvent.getOperationType()))
                            .findFirst()
                            .map(TrackCodeEvent::getEventDateTime)
                            .orElse(LocalDateTime.now(clock));
                    String durationTime = DateUtils.durationToString(firstDateTime, lastDateTime);

                    buf.append("${command.parcel.trackinfo.daysway}: <b>").append(durationTime).append("</b>\n");
                });

        getOptionalEventWithFieldFromEventList(trackCodeEventList, trackCodeEvent -> trackCodeEvent.getCountryFrom() != null)
                .ifPresent(event -> buf.append("${command.parcel.trackinfo.fromcountry}: <b>").append(event.getCountryFrom()).append("</b>\n"));

        getOptionalEventWithFieldFromEventList(trackCodeEventList, trackCodeEvent -> trackCodeEvent.getCountryTo() != null)
                .ifPresent(event -> buf.append("${command.parcel.trackinfo.tocountry}: <b>").append(event.getCountryTo()).append("</b>\n"));

        getOptionalEventWithFieldFromEventList(trackCodeEventList, trackCodeEvent -> trackCodeEvent.getSender() != null)
                .ifPresent(event -> buf.append("${command.parcel.trackinfo.sender}: <b>").append(event.getSender()).append("</b>\n"));

        getOptionalEventWithFieldFromEventList(trackCodeEventList, trackCodeEvent -> trackCodeEvent.getRecipient() != null)
                .ifPresent(event -> buf.append("${command.parcel.trackinfo.receiver}: <b>").append(event.getRecipient()).append("</b>\n"));

        buf.append("<a href='").append(TRACKING_ON_SITE_URL).append(barcode).append("'>${command.parcel.trackinfo.checkonwebsite}</a>\n");

        buf.append(BORDER);

        return buf.toString();
    }

    private Optional<TrackCodeEvent> getOptionalEventWithFieldFromEventList(List<TrackCodeEvent> trackCodeEventList, Predicate<TrackCodeEvent> predicate) {
        return trackCodeEventList.stream().filter(predicate).findFirst();
    }

    private String buildStringUpdateTimesInformation() {
        return buildStringUpdateTimesInformation(Instant.ofEpochMilli(botStats.getLastTracksUpdate()));
    }

    private String buildStringUpdateTimesInformation(Instant lastUpdateDateTime) {
        StringBuilder buf = new StringBuilder();

        Instant nextAutomaticUpdate = Instant.ofEpochMilli(botStats.getLastTracksUpdate())
                .plusSeconds(TrackCodeEventsTimer.FIXED_RATE_HOURS * 60L * 60L);

        buf.append("${command.parcel.lastupdate}: <b>").append(formatShortDateTime(lastUpdateDateTime)).append("</b>\n")
                .append("${command.parcel.nextupdate}: <b>").append(formatShortDateTime(nextAutomaticUpdate)).append("</b>\n");

        return buf.toString();
    }

    private static boolean isEconomyMode(int requestsLimit) {
        final int freePostAccountRequestsLimit = 100;
        return freePostAccountRequestsLimit >= requestsLimit;
    }

}
