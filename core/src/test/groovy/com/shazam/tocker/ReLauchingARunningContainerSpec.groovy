package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import spock.lang.Specification


class LauchingARunningContainerSpec extends Specification implements DockerDsl {
    def "will relaunch of port mapping has changed"() {
        given:
            def builder = DockerInstance
                    .fromImage("redis")
                    .withContainerName(containerNameFor("changed-config-spec"))
                    .mappingPorts(PortMap.of(6379, 6379))
            builder.build().run()

        when:
            def instance = builder.mappingPorts(PortMap.of(6380, 6379)).build().run()

        then:
            instance.mappedPorts().forContainerPort(6379) == 6380
    }
}