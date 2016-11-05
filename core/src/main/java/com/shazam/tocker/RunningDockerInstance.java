package com.shazam.tocker;

import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(fluent = true)
@Getter
public class RunningDockerInstance {
    private final MappedPorts mappedPorts;

    public static RunningDockerInstance from(ContainerInfo containerInfo) {
        Map<String, List<PortBinding>> portBindings = containerInfo.hostConfig().portBindings();
        return new RunningDockerInstance(mappedPortsFrom(portBindings));
    }

    private static MappedPorts mappedPortsFrom(Map<String, List<PortBinding>> portBindings) {
        MappedPorts.MappedPortsBuilder mappedPortsBuilder = MappedPorts.builder();
        if (portBindings != null) {
            Map<Integer, Integer> portMappings = portBindings.entrySet().stream().collect(toMap(
                    binding -> Integer.parseInt(binding.getKey().substring(0, binding.getKey().indexOf("/"))),
                    binding -> Integer.parseInt(binding.getValue().stream().findFirst().get().hostPort())));
            mappedPortsBuilder.portMaps(portMappings);
        }
        return mappedPortsBuilder.build();
    }
}
