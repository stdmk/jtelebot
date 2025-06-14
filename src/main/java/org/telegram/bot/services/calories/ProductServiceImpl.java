package org.telegram.bot.services.calories;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;
import org.telegram.bot.repositories.calories.ProductRepository;

import java.util.ArrayList;
import java.util.List;

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
    public List<Product> find(User user, String name, int size) {
        List<Product> results = new ArrayList<>(size);
        for (String word : name.split(" ")) {
            if (results.size() >= size) {
                break;
            }

            results.addAll(productRepository.findAllByUserAndNameContainingIgnoreCase(user, word));
        }

        return results;
    }

    @Override
    public void remove(Product product) {
        productRepository.delete(product);
    }
}
