package com.shazam.tocker;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerInstance {

    private final String imageName;
    private final String containerName;
    private final ImageStrategy imageStrategy;
    private HostConfig hostConfig;
    private String[] env;
    private Path dirToCustomDockerFile;
    private DefaultDockerClient dockerClient;
    private AliveStrategy NOOP_UPCHECK = AliveStrategies.alwaysAlive();

    private DockerInstance(String imageName, String containerName, HostConfig hostConfig, String[] env, Path dirToCustomDockerFile, ImageStrategy imageStrategy) {
        this.imageName = imageName;
        this.containerName = containerName;
        this.hostConfig = hostConfig;
        this.env = env;
        this.dirToCustomDockerFile = dirToCustomDockerFile;

        try {
            dockerClient = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            throw new RuntimeException(e);
        }

        this.imageStrategy = imageStrategy;
    }

    public static DockerInstanceBuilder fromImage(String imageName) {
        return new DockerInstanceBuilder(imageName);
    }

    public static DockerInstanceBuilder fromFile(Path dir, String imageName) {
        return new DockerInstanceBuilder(dir, imageName);
    }

    public String host() {
        return dockerClient.getHost();
    }

    public void run() {
        withClient((client) -> {
            try {
                startContainerIfNecessary(client, client.inspectContainer(containerName), NOOP_UPCHECK);
            } catch (DockerException de) {
                ensureImageExists(client);
                ContainerCreation container = createContainer(client);
                startContainer(client, container.id(), NOOP_UPCHECK);
            }
        });
    }

    public void run(AliveStrategy aliveStrategyCheck) {
        withClient((client) -> {
            try {
                startContainerIfNecessary(client, client.inspectContainer(containerName), aliveStrategyCheck);
            } catch (DockerException de) {
                ensureImageExists(client);
                ContainerCreation container = createContainer(client);
                startContainer(client, container.id(), aliveStrategyCheck);
            }
        });
    }

    private void startContainerIfNecessary(DockerClient client, ContainerInfo containerInfo, AliveStrategy upCheck) throws DockerException, InterruptedException {
        if (containerInfo.state().running() == false) {
            startContainer(client, containerInfo.id(), upCheck);
        }
    }


    private void startContainer(DockerClient client, String containerId, AliveStrategy upCheck) throws DockerException, InterruptedException {
        client.startContainer(containerId);
        upCheck.waitUntilAlive();
    }

    private ContainerCreation createContainer(DockerClient client) throws DockerException, InterruptedException {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig)
                .env(env)
                .build();
        return client.createContainer(containerConfig, containerName);
    }

    private void ensureImageExists(DockerClient client) throws Exception {
        try {
            client.inspectImage(imageName);
        } catch (ImageNotFoundException infe) {
            imageStrategy.loadImage(client);
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
        private final ImageStrategy imageStrategy;
        private Path dirToCustomDockerFile;
        private String imageName;
        private String containerName;
        private HostConfig hostConfig;
        private String[] env;

        public DockerInstanceBuilder(String imageName) {
            this.imageName = imageName;
            this.imageStrategy = (client) -> client.pull(imageName);
        }

        public DockerInstanceBuilder(Path dirToCustomDockerFile, String imageName) {
            this.imageName = imageName;
            this.imageStrategy = (client) -> client.build(dirToCustomDockerFile, imageName);
            this.dirToCustomDockerFile = dirToCustomDockerFile;
        }

        public DockerInstance build() {
            return new DockerInstance(imageName, containerName, hostConfig, env, dirToCustomDockerFile, imageStrategy);
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

        public DockerInstanceBuilder withEnv(String ... env) {
            this.env = env;
            return this;
        }
    }

    private static interface DockerCommand {
        public void runCommand(DockerClient dc) throws Exception;
    }

    private static interface ImageStrategy {
        public void loadImage(DockerClient dc) throws Exception;
    }
}
