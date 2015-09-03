package com.shazam.tocker.redis

import redis.clients.jedis.Jedis
import spock.lang.Specification

class RedisSpecification extends Specification {
    def "can be brought up on the specified port"() {
        given:
            def redis = new RedisInstance(6381, "tocker-redis-6380")
            redis.isNotRunning()
        when:
            redis.isRunning()
        then:
            new Jedis(redis.host(), redis.port()).ping() == "PONG"
    }
}
