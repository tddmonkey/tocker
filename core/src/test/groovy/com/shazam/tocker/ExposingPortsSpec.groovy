package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import com.spotify.docker.client.messages.PortBinding
import spock.lang.Specification


class ExposingPortsSpec extends Specification implements DockerDsl {
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
}