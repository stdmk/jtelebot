package org.telegram.bot.services.calories;

import org.telegram.bot.domain.entities.calories.EatenProduct;

public interface EatenProductService {
    EatenProduct save(EatenProduct eatenProduct);
    EatenProduct get(Long id);
    void remove(EatenProduct eatenProduct);
}
