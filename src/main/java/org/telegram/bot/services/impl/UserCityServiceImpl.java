package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.repositories.UserCityRepository;
import org.telegram.bot.services.UserCityService;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCityServiceImpl implements UserCityService {

    private final UserCityRepository userCityRepository;

    @Override
    public UserCity get(Long userCityId) {
        log.debug("Request to get UserCity by userCityId: {}", userCityId);
        return userCityRepository.findById(userCityId).orElse(null);
    }

    @Override
    public UserCity get(User user, Chat chat) {
        log.debug("Request to get UserCity by User: {} and chatId: {}", user, chat);
        return userCityRepository.findByUserAndChat(user, chat);
    }

    @Override
    public List<UserCity> getAll(City city) {
        log.debug("Request to get all UserCity by City: {}", city);
        return userCityRepository.findByCity(city);
    }

    @Override
    public void save(UserCity userCity) {
        log.debug("Request to save UserCity: {}", userCity);
        userCityRepository.save(userCity);
    }

    @NotNull
    @Override
    public ZoneId getZoneIdOfUserOrDefault(Message message) {
        return this.getZoneIdOfUserOrDefault(message.getChat(), message.getUser());
    }

    @Override
    @NotNull
    public ZoneId getZoneIdOfUserOrDefault(Chat chat, User user) {
        ZoneId zoneId = getZoneIdOfUser(chat, user);
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }

        return zoneId;
    }

    @Override
    public ZoneId getZoneIdOfUser(Chat chat, User user) {
        log.debug("Request to get ZoneId of User {} for Chat {}", user, chat);

        UserCity userCity = this.get(user, chat);
        if (userCity != null) {
            return ZoneId.of(userCity.getCity().getTimeZone());
        }

        return null;
    }

}
