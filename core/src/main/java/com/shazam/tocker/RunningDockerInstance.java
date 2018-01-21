package com.shazam.tocker;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(fluent = true)
public class RunningDockerInstance {
    @Getter private final MappedPorts mappedPorts;
    @Getter private final Map<String,String> environmentVariables;
    private final DockerClient dockerClient;
    private final ContainerInfo containerInfo;

    public static RunningDockerInstance from(ContainerInfo containerInfo, DefaultDockerClient dockerClient) {
        Map<String, List<PortBinding>> portBindings = containerInfo.networkSettings().ports();
        return new RunningDockerInstance(mappedPortsFrom(portBindings), environmentVariablesFrom(containerInfo.config()), dockerClient, containerInfo);
    }

    private static Map<String, String> environmentVariablesFrom(ContainerConfig containerConfig) {
        return Optional.ofNullable(containerConfig.env())
            .orElse(ImmutableList.of())
            .stream()
            .map(envVar -> envVar.split("="))
            .collect(Collectors.toMap(strings -> strings[0], strings -> strings[1]));
    }

    @SneakyThrows
    public void stop() {
        dockerClient.stopContainer(containerInfo.name(), 10);
    }

    private static MappedPorts mappedPortsFrom(Map<String, List<PortBinding>> portBindings) {
        MappedPorts.MappedPortsBuilder mappedPortsBuilder = MappedPorts.builder();
        if (portBindings != null) {
            Map<Integer, Integer> portMappings = portBindings.entrySet().stream().filter(e -> e.getValue().size() > 0).collect(toMap(
                binding -> Integer.parseInt(binding.getKey().substring(0, binding.getKey().indexOf("/"))),
                binding -> Integer.parseInt(binding.getValue().stream().findFirst().get().hostPort())));
            mappedPortsBuilder.portMaps(portMappings);
        }
        return mappedPortsBuilder.build();
    }

    public String getIpAddress() {
        return containerInfo.networkSettings().ipAddress();
    }
}
