package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import spock.lang.Specification


class ReLauchingARunningContainerSpec extends Specification implements DockerDsl {
    def "will rebuild container if port mapping has changed"() {
        given:
            def builder = DockerInstance
                    .fromImage("redis")
                    .withContainerName(containerNameFor("changed-config-spec"))
                    .mappingPorts(PortMap.of(6379, 6379))
            builder.build().run()

        when:
            def instance = builder.mappingPorts(PortMap.of(6379, 6380)).build().run()

        then:
            instance.mappedPorts().forContainerPort(6379) == 6380
    }

    def "will rebuild container if image has changed"() {
        given:
            def containerName = containerNameFor("changed-config-spec")
            DockerInstance
                    .fromImage("redis:2.8")
                    .withContainerName(containerName)
                    .build()
                    .run()

        when:
            DockerInstance.fromImage("redis:3.0")
                .withContainerName(containerName)
                .build()
                .run()

        then:
            client.inspectContainer(containerName).config().image() == "redis:3.0"
    }
}