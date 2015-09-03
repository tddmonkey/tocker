package com.shazam.tocker.redis;

import com.shazam.docker.DockerInstance;
import com.shazam.docker.PortMap;

public class RedisInstance {
    private final DockerInstance redis;
    private final int port;

    public RedisInstance(int port, String containerName) {
        this.port = port;
        redis = DockerInstance
                .fromImage("redis")
                .withContainerName(containerName)
                .mappingPorts(PortMap.of(6379, this.port))
                .build();
    }

    public void isRunning() {
        redis.run();
    }

    public void isNotRunning() {
        redis.stop();
    }

    public String host() {
        return redis.host();
    }

    public int port() {
        return port;
    }
}
