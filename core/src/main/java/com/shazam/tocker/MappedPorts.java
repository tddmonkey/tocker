package com.shazam.tocker;

import lombok.Builder;

import java.util.Map;
import java.util.Optional;

@Builder
public class MappedPorts {
    private final Map<Integer, Integer> portMaps;

    public int forContainerPort(int port) {
        return Optional.ofNullable(portMaps.get(port))
            .orElseThrow(() -> new IllegalArgumentException(String.format("Port %d is not mapped", port)));
    }
}
