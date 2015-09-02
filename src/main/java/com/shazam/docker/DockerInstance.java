package com.shazam.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificateException;

public class DockerInstance {

    private DefaultDockerClient dockerClient;

    public DockerInstance(String imageName, String containerName) {
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

    public void isRunning() {
//        dockerClient.
    }

    public String containerName() {
        return "";
    }

    private static class DockerInstanceBuilder {
        private String imageName;
        private String containerName;

        public DockerInstanceBuilder(String imageName) {
            this.imageName = imageName;
        }

        public DockerInstance build() {
            return new DockerInstance(imageName, containerName);
        }

        public DockerInstanceBuilder withContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }
    }
}
