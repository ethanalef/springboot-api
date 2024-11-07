package alef.proj.scheduler;

import alef.proj.model.Product;
import alef.proj.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CacheUpdateScheduler {
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;
    private final CacheManager caffeineCacheManager;
    private static final String REDIS_KEY_PREFIX = "product:";

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updateCaches() {
        List<Product> products = productRepository.findAll();
        
        // Update Redis (L2 cache)
        products.forEach(product -> {
            String redisKey = REDIS_KEY_PREFIX + product.getId();
            redisTemplate.opsForValue().set(redisKey, product, 30, TimeUnit.MINUTES);
        });

        // Clear Caffeine (L1 cache) to force refresh
        caffeineCacheManager.getCache("products").clear();
    }
}