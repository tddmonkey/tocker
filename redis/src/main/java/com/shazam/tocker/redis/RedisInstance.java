package com.shazam.tocker.redis;

import com.shazam.tocker.AliveStrategies;
import com.shazam.tocker.DockerInstance;
import com.shazam.tocker.PortMap;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.function.Supplier;

public class RedisInstance {
    public static final String REDIS_IMAGE = "redis";
    private int port = 6385;
    private DockerInstance instance;

    public RedisInstance(int port, String instanceName) {
        this.port = port;

        this.instance = DockerInstance
                .fromImage(REDIS_IMAGE)
                .mappingPorts(PortMap.of(6379, port))
                .withContainerName(instanceName)
                .build();
    }

    RedisInstance isRunning() {
        instance.run(AliveStrategies.retrying(() -> isCurrentlyRunning(), 50, 10));
        return this;
    }

    private boolean isCurrentlyRunning() {
        try {
            System.out.println("Pinging Redis to see if it's alive");
            new Jedis(instance.host(), port).ping();
        } catch (JedisException ex) {
            System.out.println("--- DEAD");
            return false;
        }
        return true;
    }

    String host() {
        return instance.host();
    }

    int port() {
        return port;
    }

    void stop() {
        instance.stop();
    }

    void isDown() {
        stop();
    }
}
