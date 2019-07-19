Welcome to Tocker

This project is designed to help the development process when using Docker containers by removing most of the boilerplate required for spinning up and shutting down Docker containers.

# Creating a Docker instance

```
this.instance = DockerInstance
                .fromImage("redis")
                .mappingPorts(PortMap.of(6379, 6380))
                .withContainerName("my-redis-container")
                .build()
```

This will create a Docker instance for the latest version of Redis and bind the Redis default port of 6379 to the local host on port 6380, meaning you can access Redis on port 6380 on your host.  Note that the container will not be started, you've just created the instance.

When running from within an IDE you need to ensure the Docker environment variables are set and exposed in order for the DockerClient to run correctly.

# Starting/Stopping an instance

To start an instance:

```
instance.run()
```

To stop an instance:

```
instance.stop()
```

Starting a container is pretty much an asychronous operation- control will be returned once the start operation has been accepted, but whatever your container is running might not yet be available. For example, mysql on my MBP takes 11 seconds to start.  During tests you will more than likely not want to return from the `run()` call until the service is actually ready to receive requests.  The `run()` method is overloaded to accept an `AliveStrategy` which defines a single method that will block until ready.  For Redis this strategy is a `retrying` strategy that attempts to connect to Redis.

```
instance.run(AliveStrategies.retrying(() -> redisIsAcceptingConnections(), 10, 100);

private Boolean redisIsAcceptingConnections() {
	try {
		new Jedis(instance.host(), 6380);
        } catch (JedisException ex) {
		return false;	
	}
	return true;
}
```

This will run the `redisIsAcceptingConnections` function every 100 milliseconds for a maximum of 10 times before giving up.  As soon as Redis is ready, the `run()` method will return control.

# Mapping Ports

In order to access the service in a container you will need to map ports to your local machine.  This can be done when creating the container.

```
this.instance = DockerInstance
                .fromImage("redis")
                .mappingPorts(PortMap.of(6379, 6380))
                .withContainerName("my-redis-container")
                .build()
```

The call to `mappingPorts` takes a list so you can map more than one port if necessary

## Ephemeral Ports

As of version 0.0.12 it is possible to use ephemeral ports when doing the mapping.  This is done by omitting the host port information in the map.

```
this.instance = DockerInstance
                .fromImage("redis")
                .mappingPorts(PortMap.ephemeral(6379))
                .withContainerName("my-redis-container")
                .build()
```

Note that specifying a host port of `0` *will not work*.

In order to discover the ports used, after launching the container the port map can be inspected like this:

```
RunningDockerInstance instance = DockerInstance
                .fromImage("redis")
                .mappingPorts(PortMap.of(6379))
                .withContainerName("my-redis-container")
                .build();
                
int mappedPort = instance.mappedPorts().forContainerPort(6379);  
```

## Exposed Ports

As of version 0.0.18 the ports exposed by an image will be automatically added based on the port mappings provided.  Prior to using this version if you try to map a port that isn't configured via the `EXPOSE` directive in the Dockerfile, it won't be mapped.

## Binding Mounts

As of cersion 0.0.21 it is possible to mount a local filesystem into the container.  This is useful when running a container that needs local data providing.  This follows the same syntax as it would on the command line:

```
RunningDockerInstance instance = DockerInstance
                .fromImage("nginx")
                .mappingPorts(PortMap.ephemeral(80))
                .withContainerName("my-nginx-container")
                .binding("/path/to/local/content:/usr/share/nginx/html:ro")
                .build();
```

# Fetching host information

When running Docker on a Windows or Mac you might be making use of boot2docker or docker-machine, both of which mean that you cannot access the container on localhost.  Because of this a Docker instance will return the host you need to connect to.  Make sure you always use this rather than hardcoding an IP address which may change between machines.

# Communication Between Containers

As of 0.0.19, `RunningDockerInstance` provides the `internalIPAddress` method, which returns the container's IP on the docker subnet. Using this with original (not remapped) ports allows communication between containers.

For example, if you had started a `mysql` container with a port mapping from `3306->32768`, you would access the container with `localhost:32768` from your host machine, but via `<internalIPAddress>:3306` from inside another container.

# Recommendations

Don't tear down the containers at the end of tests! You will incurr severe costs in terms of time for running your tests.  Containers are lightweight so can be left running during the development process.  Each test can start the container and if it is already alive you should not notice any difference to running a local instance of that infrastructure.  When starting a container the `AliveStrategy` is first consulted to see if anything actually has to happen.  With the Redis example above, once the container is running your tests run at the speed of unit tests.

# Downloading
## Maven
```
<dependency>
   <groupId>com.github.tddmonkey</groupId>
   <artifactId>tocker-core</artifactId>
   <version>0.0.21</version>
</dependency>
```

## Gradle
```
com.github.tddmonkey:tocker-core:0.0.21
```

# Building
tocker is built using the Gradle wrapper and uses Spock for tests

```
$ ./gradlew test
```
# Change Log

**Version 0.0.22 (2019-07-19)**
* Added support for `links()` so that containers can resolve each other by name or alias

**Version 0.0.21 (2018-05-14)**
* Upped version of Spotify Docker client to 8.12.2
* Upped version of Gradle to 4.7
* Added support for `binding()` mounts

**Version 0.0.20 (2018-02-06)**
* Upped version of Spotify Docker client to 8.11.1

**Version 0.0.19 (2018-01-22)**
* Can now retrieve internal Docker IP address for inter-container communication

**Version 0.0.18 (2018-01-05)**

* Can now get env variables from a running container
* Automatically adds port bindings to exposed ports

**Version 0.0.17 (2017-12-21)**

* Added ability to override CMD run on container start

**Version 0.0.16 (2017-10-26)**

* Upped version of Spotify Docker client to 8.9.1

**Version 0.0.15 (2017-01-30)**

* `stop()` method moved to `RunningDockerInstance`

**Version 0.0.14 (2016-11-13)**

* Now passes an instance of `RunningDockerInstance` to an `AliveStrategy` so that ephemeral ports can be used when doing up checks

**Version 0.0.12 (2016-11-05)**

* Added ability to get port map information from the running instance
* Added ability to use ephemeral ports in port mappings
* Upgraded to Gradle 3.1

**Version 0.0.11 (2016-09-11)**

* Upgraded Spotify client to 5.0.2
* Added exception handling up-check
* Fixed test on case-insensitive file systems
* Upgraded to Gradle 3.0
* Forked from the Shazam version
