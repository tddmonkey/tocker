package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import spock.lang.Specification


class ExposesEnvironmentVariablesSpec extends Specification implements DockerDsl {
    
    def 'Environment variables specified at container creation time are available on the RunningInstance'() {
        given:
            def containerName = containerNameFor('expose-env-variable')
            def instance = DockerInstance
                .fromImage("redis")
                .withContainerName(containerName)
                .withEnv("DUMMY_ENV_VARIABLE=bob")
                .build()
        
        expect:
            instance.run().environmentVariables().DUMMY_ENV_VARIABLE == 'bob'
    }
    
    def 'Environment variables are returned from the original container if an already-running container is retrieved'() {
        given:
            def containerName = containerNameFor('retrieve-old-env-variable')
        
        and:
            def firstInstance = DockerInstance
                .fromImage("redis")
                .withContainerName(containerName)
                .withEnv("DUMMY_ENV_VARIABLE=bob")
                .build()
        
        and:
            def secondInstance = DockerInstance
                .fromImage("redis")
                .withContainerName(containerName)
                .withEnv("DUMMY_ENV_VARIABLE=harry")
                .build()
        
        when:
            firstInstance.run()
    
        then:
            secondInstance.run().environmentVariables().DUMMY_ENV_VARIABLE == 'bob'
    }
    
}