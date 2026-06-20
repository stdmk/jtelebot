package org.telegram.bot.services.calories;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;
import org.telegram.bot.repositories.calories.ProductRepository;

import java.util.Collection;
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
    public List<Product> get(User user, String name) {
        return productRepository.findAllByUserAndNameIgnoreCaseAndDeleted(user, name, false);
    }

    @Override
    public Page<Product> find(String name, int size) {
        Specification<Product> specification = ProductSpecifications.notDeleted();

        for (String word : name.split("\\s+")) {
            specification = specification.and(ProductSpecifications.nameContains(word));
        }

        return productRepository.findAll(specification, PageRequest.of(0, size));
    }
    @Override
    public Collection<Product> find(User user, String name, int size) {
        Specification<Product> specification = Specification.where(
                        ProductSpecifications.byUser(user))
                .and(ProductSpecifications.notDeleted());

        for (String word : name.split("\\s+")) {
            specification = specification.and(
                    ProductSpecifications.nameContains(word));
        }

        return productRepository.findAll(specification, PageRequest.of(0, size)).getContent();
    }

    @Override
    public void remove(Product product) {
        productRepository.save(product.setDeleted(true));
    }
}
