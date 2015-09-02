package com.shazam.docker

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerException
import com.spotify.docker.client.DockerRequestException
import com.spotify.docker.client.messages.Container
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerCreation
import com.spotify.docker.client.messages.ContainerInfo
import com.spotify.docker.client.messages.ContainerState
import spock.lang.Ignore
import spock.lang.Specification

import static com.spotify.docker.client.DockerClient.ListContainersParam.allContainers

class UsingADockerInstanceSpec extends Specification {
    def client = DefaultDockerClient.fromEnv().build()
    private static final String CONTAINER_PREFIX = "dockertest"

    def setupSpec() {
        removeAllCreatedContainers()
    }

    private List<Container> removeAllCreatedContainers() {
        def client = DefaultDockerClient.fromEnv().build()
        def allContainers = client.listContainers(DockerClient.ListContainersParam.allContainers())
        allContainers.findAll { container -> container.names().any { name -> name.startsWith("/$CONTAINER_PREFIX") }
            .each {
                client.stopContainer(container.id(), 0)
                client.removeContainer(container.id())
            }
        }
    }

    def "provides host information"() {
        given:
            def dockerInstance = DockerInstance.fromImage("scratch").build()
            def host = client.host
        expect:
            dockerInstance.host() == host
    }

    def "can start an existing stopped container"() {
        given:
            def containerName = containerNameFor("redis")
            ensureContainerExistsFor(image: "redis", containerName: containerName)
            def dockerInstance = DockerInstance.fromImage("redis").withContainerName(containerName).build()
        when:
            dockerInstance.isRunning()
        then:
            def container = client.inspectContainer(containerName)
            assert container.state().running() == true
    }

    def containerNameFor(String name) {
        return "$CONTAINER_PREFIX-$name-" + UUID.randomUUID().toString()
    }

    def ensureContainerExistsFor(def map) {
        def containerName = map['containerName']

        ContainerConfig containerConfig = ContainerConfig.builder()
                .image(map['image'])
                .build()

        def containerId
            containerId = client.createContainer(containerConfig, containerName).id()
            client.startContainer(containerId)
    }

    private ensureImageExists(DefaultDockerClient client) {
        try {
            client.inspectImage("redis")
        } catch (DockerException de) {
            client.pull("redis")
        }
    }


    @Ignore("this should be a specific test after we're sure we can start an image")
    def "will pull the specified image if it doesn't exist locally"() {
        given:
            def client = DefaultDockerClient.fromEnv().build()
//            client.listImages()
            client.removeImage("scratch")
            def dockerInstance = DockerInstance.fromImage("scratch").build()
        when:
            dockerInstance.isRunning()
        then:
            client.inspectImage("scratch")
            notThrown(Exception)

    }
}
