package org.telegram.bot.repositories.calories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByUserAndNameIgnoreCaseAndDeleted(User user, String name, boolean deleted);
    Page<Product> findAllByNameContainingIgnoreCaseAndDeleted(String name, boolean deleted, Pageable pageable);
    List<Product> findAllByUserAndNameContainingIgnoreCaseAndDeleted(User user, String name, boolean deleted, Pageable pageable);
}
