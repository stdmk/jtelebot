package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.CityRepository;
import org.telegram.bot.services.CityService;

import java.util.List;

@Service
@AllArgsConstructor
public class CityServiceImpl implements CityService {

    private final Logger log = LoggerFactory.getLogger(CityServiceImpl.class);

    private final CityRepository cityRepository;

    @Override
    public City get(Long cityId) {
        log.debug("Request to get City by cityId: {}", cityId);
        return cityRepository.findById(cityId).orElse(null);
    }

    @Override
    public List<City> getAll() {
        log.debug("Request to get all Cities");
        return cityRepository.findAll();
    }

    @Override
    public List<City> getAll(User user) {
        log.debug("Request to get all Cities");
        return cityRepository.findByUser(user);
    }

    @Override
    public City save(City city) {
        log.debug("Request to save City: {}", city);
        return cityRepository.save(city);
    }

    @Override
    public void remove(Long cityId) {
        log.debug("Request to delete City by cityId: {}", cityId);
        cityRepository.deleteById(cityId);
    }

    @Override
    public void remove(City city) {
        log.debug("Request to delete City: {}", city);
        cityRepository.delete(city);
    }
}
