package org.telegram.bot.services.calories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.calories.EatenProduct;
import org.telegram.bot.repositories.calories.EatenProductRepository;

@RequiredArgsConstructor
@Service
public class EatenProductServiceImpl implements EatenProductService {

    private final EatenProductRepository eatenProductRepository;

    @Override
    public EatenProduct save(EatenProduct eatenProduct) {
        return eatenProductRepository.save(eatenProduct);
    }

    @Override
    public EatenProduct get(Long id) {
        return eatenProductRepository.findById(id).orElse(null);
    }

    @Override
    public void remove(EatenProduct eatenProduct) {
        eatenProductRepository.delete(eatenProduct);
    }

}
