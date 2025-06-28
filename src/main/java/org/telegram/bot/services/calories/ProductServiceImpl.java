package org.telegram.bot.services.calories;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;
import org.telegram.bot.repositories.calories.ProductRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public void save(Product product) {
        productRepository.save(product);
    }

    @Override
    public Product get(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public Product get(User user, String name) {
        return productRepository.findByUserAndNameIgnoreCase(user, name);
    }

    @Override
    public Page<Product> find(String name, int size) {
        return productRepository.findAllByNameContainingIgnoreCase(name, PageRequest.of(0, size));
    }

    @Override
    public Collection<Product> find(User user, String name, int size) {
        Map<Long, Product> results = new HashMap<>(size);
        for (String word : name.split(" ")) {
            if (results.size() >= size) {
                break;
            }

            List<Product> foundProducts = productRepository.findAllByUserAndNameContainingIgnoreCase(user, word);

            for (Product foundProduct : foundProducts) {
                if (results.containsKey(foundProduct.getId())) {
                    continue;
                }

                results.put(foundProduct.getId(), foundProduct);
            }
        }

        return results.values();
    }

    @Override
    public void remove(Product product) {
        productRepository.delete(product);
    }
}
