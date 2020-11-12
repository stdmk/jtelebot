package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.repositories.UserCityRepository;
import org.telegram.bot.services.UserCityService;

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
    public UserCity save(UserCity userCity) {
        log.debug("Request to save UserCity: {}", userCity);
        return userCityRepository.save(userCity);
    }
}
