package com.navigation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 *
 * 解决问题:
 * 1. 确保StringRedisTemplate使用正确的序列化器
 * 2. 确保能够读取Jedis或其他客户端写入的数据
 * 3. 支持UTF-8中文key和value
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:47.96.179.70}")
    private String redisHost;

    @Value("${spring.data.redis.port:8109}")
    private int redisPort;

    @Value("${spring.data.redis.password:~gmb7GK%aviH!518aU%8}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * 自定义RedisConnectionFactory
     * 强制使用正确的配置
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("[RedisConfig] 创建自定义RedisConnectionFactory");
        log.info("[RedisConfig] 配置: host={}, port={}, database={}, password={}",
            redisHost, redisPort, redisDatabase, redisPassword != null ? "已设置" : "未设置");

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        log.info("[RedisConfig] ✅ RedisConnectionFactory创建成功");
        return factory;
    }

    /**
     * 配置StringRedisTemplate
     * 使用StringRedisSerializer来处理所有的key和value
     */
    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        log.info("[RedisConfig] 开始初始化StringRedisTemplate");
        log.info("[RedisConfig] ConnectionFactory类型: {}", factory.getClass().getName());

        // 如果是LettuceConnectionFactory,打印详细配置
        if (factory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) {
            org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory lettuceFactory =
                (org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) factory;

            log.info("[RedisConfig] Lettuce配置信息:");
            log.info("[RedisConfig]   - Host: {}", lettuceFactory.getHostName());
            log.info("[RedisConfig]   - Port: {}", lettuceFactory.getPort());
            log.info("[RedisConfig]   - Database: {}", lettuceFactory.getDatabase());
            log.info("[RedisConfig]   - Password: {}", lettuceFactory.getPassword() != null ? "已设置" : "未设置");
        }

        StringRedisTemplate template = new StringRedisTemplate(factory);

        // 使用StringRedisSerializer序列化所有的key和value
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 配置key的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // 配置value的序列化方式
        template.setValueSerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);

        // 启用事务支持
        template.setEnableTransactionSupport(false);

        template.afterPropertiesSet();

        // 测试连接并检查所有数据库
        try {
            // 先检查当前连接的database
            Long dbSize = template.getConnectionFactory().getConnection().dbSize();
            log.info("[RedisConfig] ✅ 当前database大小={} keys", dbSize);

            // 如果当前database为空,尝试检查其他database
            if (dbSize == 0) {
                log.warn("[RedisConfig] ⚠️ 当前database为空,尝试检查database 0-3...");

                // 使用Jedis测试其他database
                try {
                    redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis(
                        factory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory ?
                        ((org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory)factory).getHostName() : "47.96.179.70",
                        factory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory ?
                        ((org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory)factory).getPort() : 8109
                    );

                    String password = factory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory ?
                        ((org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory)factory).getPassword() : null;

                    if (password != null && !password.isEmpty()) {
                        jedis.auth(password);
                    }

                    for (int db = 0; db < 4; db++) {
                        jedis.select(db);
                        Long size = jedis.dbSize();
                        log.info("[RedisConfig]   Database {}: {} keys", db, size);
                    }
                    jedis.close();
                } catch (Exception e) {
                    log.error("[RedisConfig] 无法检查其他database", e);
                }
            }

            // 测试读取一个key
            var keys = template.keys("scenic:*:*");
            if (keys != null && !keys.isEmpty()) {
                String firstKey = keys.iterator().next();
                String value = template.opsForValue().get(firstKey);
                log.info("[RedisConfig] ✅ 测试读取成功 | key={} | value长度={}",
                    firstKey, value != null ? value.length() : 0);
            } else {
                log.warn("[RedisConfig] ⚠️ 未找到scenic:*:*的keys");
            }
        } catch (Exception e) {
            log.error("[RedisConfig] ❌ StringRedisTemplate测试连接失败", e);
        }

        return template;
    }

    /**
     * 配置RedisTemplate (用于存储Object对象)
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        log.info("[RedisConfig] 初始化RedisTemplate");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 使用StringRedisSerializer序列化key
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 配置key的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // 配置value的序列化方式(也使用String,因为你存的是JSON字符串)
        template.setValueSerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);

        template.afterPropertiesSet();

        log.info("[RedisConfig] RedisTemplate初始化完成");
        return template;
    }
}
