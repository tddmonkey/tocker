package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import com.spotify.docker.client.messages.PortBinding
import spock.lang.Specification


class ExposingPortsSpec extends Specification implements DockerDsl {
    private static final int SERVICE1_CONTAINER_PORT = 9324
    private static final int SERVICE2_CONTAINER_PORT = 6660
    private static final int NOT_EXPOSED_BY_DEFAULT = 43728
    
    DockerInstance.DockerInstanceBuilder dockerInstance
    String containerName
    RunningDockerInstance runningInstance

    def setup() {
        containerName = containerNameFor("exposes-multiple-ports")
        dockerInstance = DockerInstance
                .fromImage("tddmonkey/elasticmq-saboteur")
                .withContainerName(containerName)
                .mappingPorts(PortMap.of(SERVICE1_CONTAINER_PORT, 2180), PortMap.of(SERVICE2_CONTAINER_PORT, 9093))
    }

    def "exposes multiple ports to the host"() {
        when:
            containerIsRun()

        then:
            def container = client.inspectContainer(containerName)
            assert container.hostConfig().portBindings() == [
                    ("${SERVICE1_CONTAINER_PORT}/tcp".toString()):[PortBinding.of("", 2180)],
                    ("${SERVICE2_CONTAINER_PORT}/tcp".toString()):[PortBinding.of("", 9093)]
            ]
    }

    def "can retrieve mapping port information"() {
        when:
            containerIsRun()

        then:
            runningInstance.mappedPorts().forContainerPort(SERVICE1_CONTAINER_PORT) == 2180
            runningInstance.mappedPorts().forContainerPort(SERVICE2_CONTAINER_PORT) == 9093
    }

    def "errors when port is not mapped"() {
        given:
            containerIsRun()

        when:
            runningInstance.mappedPorts().forContainerPort(12345)

        then:
            thrown(IllegalArgumentException)
    }

    def "can use an ephemeral port"() {
        given:
            dockerInstance.mappingPorts(PortMap.ephemeral(SERVICE1_CONTAINER_PORT))

        when:
            containerIsRun()

        then:
            runningInstance.mappedPorts().forContainerPort(SERVICE1_CONTAINER_PORT) > 0
    }

    def "does not having mapping for ports that aren't mapped"() {
        given:
            dockerInstance.mappingPorts(PortMap.ephemeral(SERVICE1_CONTAINER_PORT))
            containerIsRun()

        when:
            runningInstance.mappedPorts().forContainerPort(SERVICE2_CONTAINER_PORT)

        then:
            thrown(IllegalArgumentException)
    }
    
    def "implicitly exposes docker-side ports when mapping is requested for non-exposed ports"() {
        given:
            dockerInstance.mappingPorts(PortMap.of(NOT_EXPOSED_BY_DEFAULT, NOT_EXPOSED_BY_DEFAULT))
        
        when:
            containerIsRun()
        
        then:
            def container = client.inspectContainer(containerName)
            assert container.config().exposedPorts().contains("${NOT_EXPOSED_BY_DEFAULT}/tcp" as String)
    }
    
    void containerIsRun() {
        this.runningInstance = dockerInstance.build().run()
    }
}