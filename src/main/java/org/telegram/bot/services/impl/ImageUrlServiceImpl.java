package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.ImageUrl;
import org.telegram.bot.repositories.ImageUrlRepository;
import org.telegram.bot.services.ImageUrlService;

@Service
@AllArgsConstructor
public class ImageUrlServiceImpl implements ImageUrlService {

    private final Logger log = LoggerFactory.getLogger(CityServiceImpl.class);

    private final ImageUrlRepository imageUrlRepository;

    @Override
    public ImageUrl get(Long imageUrlId) {
        log.debug("Request to get ImageUrl by Id: {}", imageUrlId);
        return imageUrlRepository.findById(imageUrlId).orElse(null);
    }

    @Override
    public ImageUrl save(ImageUrl imageUrl) {
        log.debug("Request to save ImageUrl: {}", imageUrl);
        return imageUrlRepository.save(imageUrl);
    }
}
