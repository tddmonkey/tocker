package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import spock.lang.Specification


class InjectingEnvironmentVariablesSpec extends Specification implements DockerDsl {
    
    def 'are made available on the RunningInstance'() {
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
    
    def 'are overriden by the original container if an already-running container is retrieved'() {
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