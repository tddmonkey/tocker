package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import spock.lang.Specification


class ReLauchingARunningContainerSpec extends Specification implements DockerDsl {
    private DockerInstance.DockerInstanceBuilder containerBuilder
    String containerName

    def setup() {
        containerName = containerNameFor("changed-config-spec")
        containerBuilder = DockerInstance
                .fromImage("redis:2.8")
                .withContainerName(containerName)
                .mappingPorts(PortMap.of(6379, 6379))
    }

    def "will rebuild container if port mapping has changed"() {
        given:
            containerBuilder.build().run()

        when:
            def instance = containerBuilder.mappingPorts(PortMap.of(6379, 6380)).build().run()

        then:
            instance.mappedPorts().forContainerPort(6379) == 6380
    }

    def "will rebuild container if image has changed"() {
        given:
            containerBuilder.build().run()

        when:
            DockerInstance.fromImage("redis:3.0")
                .withContainerName(containerName)
                .build()
                .run()

        then:
            client.inspectContainer(containerName).config().image() == "redis:3.0"
    }

    def "will rebuild existing but stopped container"() {
        given:
            containerBuilder.build().run().stop()

        when:
            DockerInstance.fromImage("redis:3.0")
                    .withContainerName(containerName)
                    .build()
                    .run()

        then:
            client.inspectContainer(containerName).config().image() == "redis:3.0"
    }
}