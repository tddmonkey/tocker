package com.shazam.tocker.dsl

import com.spotify.docker.client.DefaultDockerClient
import org.junit.BeforeClass

import static com.spotify.docker.client.DockerClient.ListContainersParam.allContainers

trait RemoveAllContainersBeforeSpec {
    abstract String containerPrefix()

    @BeforeClass
    def removeAllCreatedContainers() {
        def client = DefaultDockerClient.fromEnv().build()
        def allContainers = client.listContainers(allContainers())
        allContainers.findAll { container ->
            container.names().any { name -> name.startsWith("/${containerPrefix()}") }
        }
        .each { container ->
            client.stopContainer(container.id(), 0)
            client.removeContainer(container.id())
        }
    }
}