/*
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shazam.tocker

import com.shazam.tocker.dsl.DockerDsl
import spock.lang.Specification

import java.nio.file.Paths

class UsingADockerInstanceFromFileSpec extends Specification implements DockerDsl {
    def setupSpec() {
        removeAllCreatedContainers()
    }

    def "can launch a container from a local DockerFile"() {
        given:
            def imageName = "dockertest-custom-image"
            imageDoesNotExist(imageName)
            def customFileLocation = getClass().classLoader.getResource("dockerfiles/customtest")
            def containerName = containerNameFor("launch-custom-file")
        when:
            DockerInstance.fromFile(Paths.get(customFileLocation.toURI()), imageName)
                .withContainerName(containerName)
                .build()
                .run()
        then:
            assert client.inspectImage(imageName).created()
            assert client.inspectContainer(containerName).state().running()
    }
}
