package org.telegram.bot.services.calories;

import org.springframework.data.domain.Page;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;

import java.util.Collection;

public interface ProductService {
    void save(Product product);
    Product get(Long id);
    Product get(User user, String name);
    Page<Product> find(String name, int size);
    Collection<Product> find(User user, String name, int size);
    void remove(Product product);
}
