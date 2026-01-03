package com.hwangso.mhbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    @Bean(name = "holdRefreshScript")
    public RedisScript<Long> holdRefreshScript() {
        String lua = """
            local v = redis.call('GET', KEYS[1])
            if not v then
              return -1
            end
            if v ~= ARGV[1] then
              return 0
            end
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            return 1
        """;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }
}