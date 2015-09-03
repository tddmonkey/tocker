package com.shazam.docker;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerInstance {

    private final String imageName;
    private final String containerName;
    private HostConfig hostConfig;
    private DefaultDockerClient dockerClient;

    private DockerInstance(String imageName, String containerName, HostConfig hostConfig) {
        this.imageName = imageName;
        this.containerName = containerName;
        this.hostConfig = hostConfig;

        try {
            dockerClient = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static DockerInstanceBuilder fromImage(String imageName) {
        return new DockerInstanceBuilder(imageName);
    }

    public String host() {
        return dockerClient.getHost();
    }

    public void run() {
        withClient((client) -> {
            try {
                startContainerIfNecessary(client, client.inspectContainer(containerName));
            } catch (DockerException de) {
                ensureImageExists(client);
                ContainerCreation container = createContainer(client);
                startContainer(client, container.id());
            }
        });
    }

    private void startContainerIfNecessary(DockerClient client, ContainerInfo containerInfo) throws DockerException, InterruptedException {
        if (containerInfo.state().running() == false) {
            startContainer(client, containerInfo.id());
        }
    }


    private void startContainer(DockerClient client, String containerId) throws DockerException, InterruptedException {
        client.startContainer(containerId);
    }

    private ContainerCreation createContainer(DockerClient client) throws DockerException, InterruptedException {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig)
                .build();
        return client.createContainer(containerConfig, containerName);
    }

    private void ensureImageExists(DockerClient client) throws DockerException, InterruptedException {
        try {
            client.inspectImage(imageName);
        } catch (ImageNotFoundException infe) {
            client.pull(imageName);
        }
    }

    public void stop() {
        withClient((client) -> client.stopContainer(containerName, 10));
    }

    private void withClient(DockerCommand consumer) {
        try {
            consumer.runCommand(dockerClient);
        } catch (Exception de) {
            throw new RuntimeException(de);
        }
    }

    public static class DockerInstanceBuilder {
        private String imageName;
        private String containerName;
        private HostConfig hostConfig;

        public DockerInstanceBuilder(String imageName) {
            this.imageName = imageName;
        }

        public DockerInstance build() {
            return new DockerInstance(imageName, containerName, hostConfig);
        }

        public DockerInstanceBuilder withContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public DockerInstanceBuilder mappingPorts(PortMap portMap) {
            PortBinding portBinding = PortBinding.of("0.0.0.0", portMap.localhostPort());
            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            portBindings.put(String.format("%d/tcp", portMap.containerPort()), Arrays.asList(portBinding));
            hostConfig = HostConfig.builder().portBindings(portBindings).build();
            return this;
        }
    }

    private static interface DockerCommand {
        public void runCommand(DockerClient dc) throws Exception;
    }
}