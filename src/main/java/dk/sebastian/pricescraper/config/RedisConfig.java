package dk.sebastian.pricescraper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sebastian.pricescraper.records.ProductPriceDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ProductPriceDto> productPriceRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, ProductPriceDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new ProductPriceDtoRedisSerializer(objectMapper));
        template.afterPropertiesSet();
        return template;
    }
}
