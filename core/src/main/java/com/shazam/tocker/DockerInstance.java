/*
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shazam.tocker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.*;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.shazam.tocker.AliveStrategies.alwaysAlive;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

public class DockerInstance {
    private final String imageName;
    private final String containerName;
    private final ImageStrategy imageStrategy;
    private HostConfig hostConfig;
    private String[] env;
    private DefaultDockerClient dockerClient;

    private DockerInstance(String imageName, String containerName, HostConfig hostConfig, String[] env, ImageStrategy imageStrategy) {
        this.imageName = imageName;
        this.containerName = containerName;
        this.hostConfig = hostConfig;
        this.env = env;

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

    public RunningDockerInstance run() {
        return run(alwaysAlive());
    }

    @SneakyThrows
    public RunningDockerInstance run(AliveStrategy aliveStrategyCheck) {
            try {
                return startContainerIfNecessary(dockerClient.inspectContainer(containerName), aliveStrategyCheck);
            } catch (DockerException de) {
                ensureImageExists();
                ContainerCreation container = createContainer();
                return startContainer(container.id(), aliveStrategyCheck);
            }
    }

    private RunningDockerInstance startContainerIfNecessary(ContainerInfo containerInfo, AliveStrategy upCheck) throws DockerException, InterruptedException {
        if (!containerInfo.state().running()) {
            return startContainer(containerInfo.id(), upCheck);
        }
        return RunningDockerInstance.from(containerInfo, dockerClient);
    }

    private RunningDockerInstance startContainer(String containerId, AliveStrategy upCheck) throws DockerException, InterruptedException {
        dockerClient.startContainer(containerId);
        ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);
        RunningDockerInstance runningInstance = RunningDockerInstance.from(containerInfo, dockerClient);
        upCheck.waitUntilAlive(runningInstance);
        return runningInstance;
    }

    private ContainerCreation createContainer() throws DockerException, InterruptedException {
        ContainerConfig containerConfig = ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig)
                .env(env)
                .build();
        return dockerClient.createContainer(containerConfig, containerName);
    }

    private void ensureImageExists() throws Exception {
        try {
            dockerClient.inspectImage(imageName);
        } catch (ImageNotFoundException infe) {
            imageStrategy.loadImage(dockerClient);
        }
    }

    public static class DockerInstanceBuilder {
        private final ImageStrategy imageStrategy;
        private String imageName;
        private String containerName;
        private HostConfig.Builder hostConfig = HostConfig.builder();
        private String[] env;

        DockerInstanceBuilder(String imageName) {
            this.imageName = imageName;
            this.imageStrategy = (client) -> client.pull(imageName);
        }

        DockerInstanceBuilder(Path dirToCustomDockerFile, String imageName) {
            this.imageName = imageName;
            this.imageStrategy = (client) -> client.build(dirToCustomDockerFile, imageName);
        }

        public DockerInstance build() {
            return new DockerInstance(imageName, containerName, hostConfig.build(), env, imageStrategy);
        }

        public DockerInstanceBuilder withContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public DockerInstanceBuilder mappingPorts(PortMap ... portMaps) {
            Map<String, List<PortBinding>> portBindings = stream(portMaps).collect(toMap(
                    pm -> String.format("%d/tcp", pm.containerPort()),
                    pm -> singletonList(pm.toBinding())));
            hostConfig.portBindings(portBindings).build();
            return this;
        }

        public DockerInstanceBuilder privileged() {
            this.hostConfig.privileged(true);
            return this;
        }

        public DockerInstanceBuilder withEnv(String ... env) {
            this.env = env;
            return this;
        }
    }

    private interface ImageStrategy {
        void loadImage(DockerClient dc) throws Exception;
    }
}
