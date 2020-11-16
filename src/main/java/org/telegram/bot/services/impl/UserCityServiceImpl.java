package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.repositories.UserCityRepository;
import org.telegram.bot.services.UserCityService;

import java.util.List;

@Service
@AllArgsConstructor
public class UserCityServiceImpl implements UserCityService {

    private final Logger log = LoggerFactory.getLogger(UserCityServiceImpl.class);

    private final UserCityRepository userCityRepository;

    @Override
    public UserCity get(Long userCityId) {
        log.debug("Request to get UserCity by userCityId: {}", userCityId);
        return userCityRepository.findById(userCityId).orElse(null);
    }

    @Override
    public UserCity get(User user, Long chatId) {
        log.debug("Request to get UserCity by User: {} and chatId: {}", user, chatId);
        return userCityRepository.findByUserAndChatId(user, chatId);
    }

    @Override
    public List<UserCity> getAll(City city) {
        log.debug("Request to get all UserCity by City: {}", city);
        return userCityRepository.findByCity(city);
    }

    @Override
    public UserCity save(UserCity userCity) {
        log.debug("Request to save UserCity: {}", userCity);
        return userCityRepository.save(userCity);
    }
}
