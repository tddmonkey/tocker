package com.shazam.tocker;

import lombok.Builder;

import java.util.Map;

@Builder
public class MappedPorts {
    private final Map<Integer, Integer> portMaps;

    public int forContainerPort(int port) {
        return portMaps.get(port);
    }
}
