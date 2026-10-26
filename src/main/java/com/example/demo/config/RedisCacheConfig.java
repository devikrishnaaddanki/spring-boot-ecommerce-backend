package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
public class RedisCacheConfig {

@Bean
public RedisCacheManager cacheManager(
        RedisConnectionFactory connectionFactory) {

    GenericJacksonJsonRedisSerializer serializer =
            GenericJacksonJsonRedisSerializer.builder()
                    .enableDefaultTyping(
                            BasicPolymorphicTypeValidator.builder()
                                    .allowIfSubType("com.example.demo.entity.")
                                    .allowIfSubType("java.math.")
                                    .allowIfSubType("java.lang.")
                                    .allowIfSubType("java.util.")
                                    .build()
                    )
                    .build();


}



        RedisCacheConfiguration cacheConfiguration =
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(java.time.Duration.ofMinutes(5))
                    .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }
}
