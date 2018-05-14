package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import spock.lang.Specification

import java.nio.file.Paths


class MountingSpec extends Specification implements DockerDsl {
    String containerName
    RunningDockerInstance runningInstance

    def setup() {
        containerName = containerNameFor("mount-local-filesystem")
        def hostPath = Paths.get(getClass().classLoader.getResource("mount").toURI()).toString()
        runningInstance = DockerInstance
                .fromImage("nginx")
                .withContainerName(containerName)
                .mappingPorts(PortMap.ephemeral(80))
                .binding("${hostPath}:/usr/share/nginx/html:ro")
                .build()
                .run(AliveStrategies.retrying({ pingServer(it) }, 20, 100))
    }

    def "mounts the filesystem in the container"() {
        expect:
            fetchDataFrom(runningInstance) == "Hello World"
    }

    def pingServer(RunningDockerInstance runningDockerInstance) {
        urlFor(runningDockerInstance).openConnection()
        return true
    }

    private static String fetchDataFrom(RunningDockerInstance runningDockerInstance) {
        return urlFor(runningDockerInstance).getText(connectTimeout: 5000)
    }

    private static URL urlFor(RunningDockerInstance runningDockerInstance) {
        new URL("http://localhost:${runningDockerInstance.mappedPorts().forContainerPort(80)}/")
    }
}