package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Parcel;
import org.telegram.bot.domain.entities.TrackCode;
import org.telegram.bot.domain.entities.TrackCodeEvent;
import org.telegram.bot.services.ParcelService;
import org.telegram.bot.services.TrackCodeService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrackCodeEventsTimer extends TimerParent {
    private final Bot bot;
    private final BotStats botStats;
    private final ParcelService parcelService;
    private final TrackCodeService trackCodeService;

    public static final int FIXED_RATE_HOURS = 3;

    @Override
    @Scheduled(fixedRate = FIXED_RATE_HOURS * 60 * 60 * 1000)
    public void execute() {
        List<Parcel> parcelList = parcelService.getAll();

        Map<Long, LocalDateTime> lastEventUpdateDateTimeMap = new HashMap<>();
        parcelList
                .stream()
                .map(Parcel::getTrackCode)
                .forEach(trackCode -> lastEventUpdateDateTimeMap.put(
                        trackCode.getId(),
                        trackCode.getEvents()
                                .stream()
                                .map(TrackCodeEvent::getEventDateTime)
                                .max(LocalDateTime::compareTo)
                                .orElse(trackCode.getCreateDateTime())));

        trackCodeService.updateFromApi();

        trackCodeService.getAll().forEach(trackCodeAfter -> {
            if (Boolean.TRUE.equals(trackCodeAfter.getInvalid())) {
                getParcelListByTrackCode(parcelList, trackCodeAfter)
                        .forEach(parcel -> {
                            String messageText = "<b>" + parcel.getName() + "</b>\n" +
                                    "<code>" + parcel.getTrackCode().getBarcode() + "</code>\n" +
                                    "Нет ответа от сервиса удали /parcel_d" + parcel.getId() + " и добавь снова подходящий";

                            try {
                                SendMessage sendMessage = new SendMessage();
                                sendMessage.setChatId(parcel.getUser().getUserId());
                                sendMessage.enableHtml(true);
                                sendMessage.setText(messageText);

                                bot.execute(sendMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        });
            } else {
                LocalDateTime lastEventDateTime = lastEventUpdateDateTimeMap.get(trackCodeAfter.getId());

                trackCodeAfter.getEvents()
                        .stream()
                        .filter(event -> event.getEventDateTime().isAfter(lastEventDateTime))
                        .sorted(Comparator.comparing(TrackCodeEvent::getEventDateTime))
                        .forEach(newEvent -> getParcelListByTrackCode(parcelList, trackCodeAfter)
                                .forEach(parcel -> {
                                    String messageText = org.telegram.bot.commands.Parcel.buildStringEventMessage(parcel, newEvent);

                                    try {
                                        SendMessage sendMessage = new SendMessage();
                                        sendMessage.setChatId(parcel.getUser().getUserId());
                                        sendMessage.enableHtml(true);
                                        sendMessage.setText(messageText);

                                        bot.execute(sendMessage);
                                    } catch (TelegramApiException e) {
                                        e.printStackTrace();
                                    }
                                }));
            }
        });

        botStats.setLastTracksUpdate(Instant.now());
    }

    private List<Parcel> getParcelListByTrackCode(List<Parcel> parcelList, TrackCode trackCode) {
        return parcelList
                .stream()
                .filter(parcel -> trackCode.getId().equals(parcel.getTrackCode().getId()))
                .collect(Collectors.toList());
    }

}
