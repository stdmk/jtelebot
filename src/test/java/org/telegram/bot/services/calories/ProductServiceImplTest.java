package org.telegram.bot.services.calories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.calories.Product;
import org.telegram.bot.repositories.calories.ProductRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void findByUserAndNameTest() {
        final int count = 15;
        final String word1 = "word1";
        final String word2 = "word2";
        final String word3 = "word3";
        String name = word1 + " " + word2 + " " + word3;
        User user = TestUtils.getUser();

        when(productRepository.findAllByUserAndNameContainingIgnoreCaseAndDeleted(eq(user), eq(word1), false, any(Pageable.class)))
                .thenReturn(getProducts(10, user));
        when(productRepository.findAllByUserAndNameContainingIgnoreCaseAndDeleted(eq(user), eq(word2), false, any(Pageable.class)))
                .thenReturn(getProducts(50, user));

        Collection<Product> products = productService.find(user, name, count);

        assertEquals(count, products.size());

        ArgumentCaptor<Pageable> argumentCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAllByUserAndNameContainingIgnoreCaseAndDeleted(eq(user), eq(word1), false, argumentCaptor.capture());
        verify(productRepository).findAllByUserAndNameContainingIgnoreCaseAndDeleted(eq(user), eq(word2), false, argumentCaptor.capture());
        verify(productRepository, never()).findAllByUserAndNameContainingIgnoreCaseAndDeleted(eq(user), eq(word3), false, any(Pageable.class));
    }

    private List<Product> getProducts(long count, User user) {
        return LongStream.range(0, count).mapToObj(i -> getProduct(i, user)).toList();
    }

    private Product getProduct(long id, User user) {
        return new Product()
                .setId(id)
                .setUser(user);
    }

}