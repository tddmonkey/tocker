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

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.exceptions.DockerException
import com.spotify.docker.client.exceptions.ImageNotFoundException
import com.spotify.docker.client.messages.ContainerConfig

import static com.spotify.docker.client.DockerClient.ListContainersParam.allContainers

trait DockerDsl {
    static final String CONTAINER_PREFIX = "dockertest"
    DockerClient client = DefaultDockerClient.fromEnv().build()

    def removeAllCreatedContainers() {
        def client = DefaultDockerClient.fromEnv().build()
        def allContainers = client.listContainers(allContainers())
        allContainers.findAll { container ->
            container.names().any { name -> name.startsWith("/${this.CONTAINER_PREFIX}") }
        }
        .each { container ->
            client.stopContainer(container.id(), 0)
            client.removeContainer(container.id())
        }
    }

    def containerNameFor(String name) {
        return "${CONTAINER_PREFIX}-$name-${UUID.randomUUID().toString()}"
    }

    def imageDoesNotExist(String imageName) {
        try {
            this.client.removeImage(imageName, true, false)
        } catch (ImageNotFoundException infe) {
            // ignore this, the image doesn't exist anyway
        }
    }

    def ensureContainerExistsFor(def map) {
        def containerName = map['containerName']

        ContainerConfig containerConfig = ContainerConfig.builder()
                .image(map['image'])
                .build()
        client.createContainer(containerConfig, containerName).id()
    }

    def ensureImageExists(imageName) {
        try {
            client.inspectImage(imageName)
        } catch (DockerException de) {
            client.pull("redis")
        }
    }
}
