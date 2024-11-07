package alef.proj.service;

import alef.proj.model.Product;
import alef.proj.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;
    private static final String REDIS_KEY_PREFIX = "product:";

    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        // Try Redis (L2 cache) first
        String redisKey = REDIS_KEY_PREFIX + id;
        Product product = redisTemplate.opsForValue().get(redisKey);
        if (product != null) {
            return product;
        }

        // If not in Redis, get from DB
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            product = productOpt.get();
            // Store in Redis
            redisTemplate.opsForValue().set(redisKey, product, 30, TimeUnit.MINUTES);
            return product;
        }
        return null;
    }

    @CacheEvict(value = "products", key = "#product.id")
    public Product saveProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        // Update Redis
        String redisKey = REDIS_KEY_PREFIX + savedProduct.getId();
        redisTemplate.opsForValue().set(redisKey, savedProduct, 30, TimeUnit.MINUTES);
        return savedProduct;
    }
}