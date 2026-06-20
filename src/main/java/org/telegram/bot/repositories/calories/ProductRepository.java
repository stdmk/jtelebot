package org.telegram.bot.repositories.calories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findAllByUserAndNameIgnoreCaseAndDeleted(User user, String name, boolean deleted);
}
