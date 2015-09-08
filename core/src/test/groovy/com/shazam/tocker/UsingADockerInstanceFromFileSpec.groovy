package com.shazam.tocker

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerException
import com.spotify.docker.client.ImageNotFoundException
import com.spotify.docker.client.ProgressHandler
import com.spotify.docker.client.messages.Container
import com.spotify.docker.client.messages.ProgressMessage
import spock.lang.Specification

import java.nio.file.Paths

class UsingADockerInstanceFromFileSpec extends Specification {
    def client = DefaultDockerClient.fromEnv().build()
    private static final String CONTAINER_PREFIX = "dockertest"

    def setupSpec() {
        removeAllCreatedContainers()
    }

    private List<Container> removeAllCreatedContainers() {
        def client = DefaultDockerClient.fromEnv().build()
        def allContainers = client.listContainers(DockerClient.ListContainersParam.allContainers())
        allContainers.findAll { container ->
            container.names().any { name -> name.startsWith("/dockertest") }
        }
            .each { container ->
                println("Stopping " + container.id())
                client.stopContainer(container.id(), 0)
                client.removeContainer(container.id())
            }
    }

    def "can launch a container from a local DockerFile"() {
        given:
            def imageName = "dockertest-custom-image"
            imageDoesNotExist(imageName)
            def customFileLocation = getClass().classLoader.getResource("dockerfiles/customtest")
            def containerName = containerNameFor("launch-custom-file")
            def dockerInstance = DockerInstance.fromFile(Paths.get(customFileLocation.toURI()), imageName)
                    .withContainerName(containerName)
                    .build()
        when:
            dockerInstance.run()
        then:
            client.inspectImage(imageName).created()
            notThrown(ImageNotFoundException)
            assert client.inspectContainer(containerName).state().running() == true
    }

    def imageDoesNotExist(String imageName) {
        try {
            client.removeImage(imageName, true, false)
        } catch (ImageNotFoundException infe) {
            // ignore this, the image doesn't exist anyway
        }
    }

    def containerNameFor(String name) {
        return "$CONTAINER_PREFIX-$name-" + UUID.randomUUID().toString()
    }

    public static void main(String[] args) {
        def client = DefaultDockerClient.fromEnv().build()
        def customFileLocation = UsingADockerInstanceFromFileSpec.class.classLoader.getResource("dockerfiles/customtest")
        println(customFileLocation)
        def dir = Paths.get(customFileLocation.toURI())
        def build = client.build(dir, "my_image_name", new ProgressHandler() {
            @Override
            void progress(ProgressMessage message) throws DockerException {
                println("HELP!")
                println(message.status())
            }
        })
        println("build $build")

    }
}
