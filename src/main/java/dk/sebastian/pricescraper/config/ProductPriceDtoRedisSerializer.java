package dk.sebastian.pricescraper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sebastian.pricescraper.records.ProductPriceDto;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;

public class ProductPriceDtoRedisSerializer implements RedisSerializer<ProductPriceDto> {

    private final ObjectMapper objectMapper;

    public ProductPriceDtoRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(ProductPriceDto value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }

        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize product price DTO", e);
        }
    }

    @Override
    public ProductPriceDto deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            return objectMapper.readValue(bytes, ProductPriceDto.class);
        } catch (IOException e) {
            throw new SerializationException("Could not deserialize product price DTO", e);
        }
    }
}
