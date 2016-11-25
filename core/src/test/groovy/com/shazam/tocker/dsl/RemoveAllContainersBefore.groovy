package com.shazam.tocker.dsl

import com.spotify.docker.client.DefaultDockerClient
import org.junit.Before
import org.junit.BeforeClass

import static com.spotify.docker.client.DockerClient.ListContainersParam.allContainers

trait RemoveAllContainersBefore extends RemoveAllContainersBeforeSpec {
    abstract String containerPrefix()

    @Before
    def removeAllCreatedContainersBeforeEachTest() {
        removeAllCreatedContainers()
    }
}