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
package com.shazam.tocker

import com.spotify.docker.client.DockerException
import com.spotify.docker.client.ImageNotFoundException
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.PortBinding
import spock.lang.Specification

class UsingADockerInstanceSpec extends Specification implements DockerDsl {
    def setupSpec() {
        removeAllCreatedContainers()
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
            assert client.inspectContainer(containerName).state().running()
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
            assert !client.inspectContainer(containerName).state().running()
    }

    def "can start a non-existent container with an existing image"() {
        given:
            def containerName = containerNameFor("image-doesntexist")
            def dockerInstance = DockerInstance.fromImage("redis").withContainerName(containerName).build()
            ensureImageExists("redis")
        when:
            dockerInstance.run()
        then:
            assert client.inspectContainer(containerName).state().running()
    }

    def "will pull the image if it doesn't exist"() {
        given:
            def containerName = containerNameFor("pull")
            imageDoesNotExist("tianon/true")
            def dockerInstance = DockerInstance.fromImage("tianon/true").withContainerName(containerName).build()
        when:
            dockerInstance.run()
        then:
            assert client.inspectContainer(containerName).created() != null
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

    def "exposes multiple ports to the host"() {
        given:
            def containerName = containerNameFor("exposes-multiple-ports")
            def dockerInstance = DockerInstance
                    .fromImage("redis")
                    .withContainerName(containerName)
                    .mappingPorts(PortMap.of(2181, 2180), PortMap.of(9092, 9093))
                    .build()
        when:
            dockerInstance.run()
        then:
            def container = client.inspectContainer(containerName)
            assert container.hostConfig().portBindings() == [
                    '2181/tcp':[PortBinding.of("0.0.0.0", 2180)],
                    '9092/tcp':[PortBinding.of("0.0.0.0", 9093)]
            ]
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
            assert client.inspectContainer(containerName).state().running()
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
            assert client.inspectContainer(containerName).state().running()
    }
}
