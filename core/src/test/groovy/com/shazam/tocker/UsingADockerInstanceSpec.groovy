/*
 * Copyright (C) 2011 Google Inc.
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
 */package com.shazam.tocker

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerException
import com.spotify.docker.client.ImageNotFoundException
import com.spotify.docker.client.messages.Container
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.PortBinding
import spock.lang.Specification

import java.util.function.Supplier

class UsingADockerInstanceSpec extends Specification {
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

    def "provides host information"() {
        given:
            def dockerInstance = DockerInstance.fromImage("scratch").build()
            def host = client.host
        expect:
            dockerInstance.host() == host
    }

    def "can start an existing stopped container"() {
        given:
            def containerName = containerNameFor("start-existing")
            ensureContainerExistsFor(image: "redis", containerName: containerName)
            def dockerInstance = DockerInstance.fromImage("redis").withContainerName(containerName).build()
        when:
            dockerInstance.run()
        then:
            def container = client.inspectContainer(containerName)
            assert container.state().running() == true
    }

    def "can stop a running container"() {
        given:
            def containerName = containerNameFor("stop-running")
            ensureContainerExistsFor(image: "redis", containerName: containerName)
            def dockerInstance = DockerInstance.fromImage("redis").withContainerName(containerName).build()
            dockerInstance.run()
        when:
            dockerInstance.stop()
        then:
            def container = client.inspectContainer(containerName)
            assert container.state().running() == false

    }

    def "can start a non-existent container with an existing image"() {
        given:
            def containerName = containerNameFor("image-doesntexist")
            def dockerInstance = DockerInstance.fromImage("redis").withContainerName(containerName).build()
            ensureImageExists("redis")
        when:
            dockerInstance.run()
        then:
            def container = client.inspectContainer(containerName)
            assert container.state().running() == true
    }

    def "will pull the image if it doesn't exist"() {
        given:
            def containerName = containerNameFor("pull")
            imageDoesNotExist("tianon/true")
            def dockerInstance = DockerInstance.fromImage("tianon/true").withContainerName(containerName).build()
        when:
            dockerInstance.run()
        then:
            def container = client.inspectContainer(containerName)
            assert container.created() != null
    }

    def "exposes ports to the host"() {
        given:
            def containerName = containerNameFor("exposes-ports")
            def dockerInstance = DockerInstance
                    .fromImage("redis")
                    .withContainerName(containerName)
                    .mappingPorts(PortMap.of(6379, 6380))
                    .build()
        when:
            dockerInstance.run()
        then:
            def container = client.inspectContainer(containerName)
            assert container.hostConfig().portBindings() == ['6379/tcp':[PortBinding.of("0.0.0.0", 6380)]]
    }

    def "does not fail when starting an already running container"() {
        given:
            def containerName = containerNameFor("start-already-running")
            def dockerInstance = DockerInstance
                    .fromImage("redis")
                    .withContainerName(containerName)
                    .build()
        when:
            dockerInstance.run()
            dockerInstance.run()
        then:
            def container = client.inspectContainer(containerName)
            assert container.state().running() == true
    }

    def "only returns from run when 'up' check returns true"() {
        given:
            AliveStrategy aliveStrategy = Mock()
            def containerName = containerNameFor("start-already-running")
            def dockerInstance = DockerInstance
                    .fromImage("redis")
                    .withContainerName(containerName)
                    .build()
        when:
            dockerInstance.run(aliveStrategy)
        then:
            1 * aliveStrategy.waitUntilAlive()
            def container = client.inspectContainer(containerName)
            assert container.state().running() == true
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

    def ensureContainerExistsFor(def map) {
        def containerName = map['containerName']

        ContainerConfig containerConfig = ContainerConfig.builder()
                .image(map['image'])
                .build()
        client.createContainer(containerConfig, containerName).id()
    }

    private ensureImageExists(imageName) {
        try {
            client.inspectImage(imageName)
        } catch (DockerException de) {
            client.pull("redis")
        }
    }
}
