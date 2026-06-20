package org.telegram.bot.services.calories;

import org.springframework.data.jpa.domain.Specification;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;

public class ProductSpecifications {

    public static Specification<Product> byUser(User user) {
        return (root, query, cb) ->
                cb.equal(root.get("user"), user);
    }

    public static Specification<Product> notDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("deleted"));
    }

    public static Specification<Product> nameContains(String word) {
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("name")),
                        "%" + word.toLowerCase() + "%"
                );
    }
}
