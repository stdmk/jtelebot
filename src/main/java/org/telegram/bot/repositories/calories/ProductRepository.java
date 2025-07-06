package org.telegram.bot.repositories.calories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByUserAndNameIgnoreCase(User user, String name);
    Page<Product> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
    List<Product> findAllByUserAndNameContainingIgnoreCase(User user, String name, Pageable pageable);
}
