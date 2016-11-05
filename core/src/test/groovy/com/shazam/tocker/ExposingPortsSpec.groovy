package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import com.spotify.docker.client.messages.PortBinding
import spock.lang.Specification


class ExposingPortsSpec extends Specification implements DockerDsl {
    DockerInstance dockerInstance
    String containerName

    def setup() {
        containerName = containerNameFor("exposes-multiple-ports")
        dockerInstance = DockerInstance
                .fromImage("redis")
                .withContainerName(containerName)
                .mappingPorts(PortMap.of(2181, 2180), PortMap.of(9092, 9093))
                .build()
    }

    def "exposes multiple ports to the host"() {
        when:
            dockerInstance.run()

        then:
            def container = client.inspectContainer(containerName)
            assert container.hostConfig().portBindings() == [
                    '2181/tcp':[PortBinding.of("0.0.0.0", 2180)],
                    '9092/tcp':[PortBinding.of("0.0.0.0", 9093)]
            ]
    }

    def "can retrieve mapping port information"() {
        when:
            def runningInstance = dockerInstance.run()

        then:
            runningInstance.mappedPorts().forContainerPort(2181) == 2180
            runningInstance.mappedPorts().forContainerPort(9092) == 9093
    }
}