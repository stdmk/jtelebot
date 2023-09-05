package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.CityRepository;
import org.telegram.bot.services.CityService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;

    @Override
    public City get(Long cityId) {
        log.debug("Request to get City by cityId: {}", cityId);
        return cityRepository.findById(cityId).orElse(null);
    }

    @Override
    public City get(String cityName) {
        log.debug("Request to get City by name: {}", cityName);
        return cityRepository.findFirstByNameRuIgnoreCaseOrNameEnIgnoreCase(cityName, cityName);
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
    public void save(City city) {
        log.debug("Request to save City: {}", city);
        cityRepository.save(city);
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
