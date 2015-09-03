package com.shazam.tocker;

public class PortMap {
    private final int containerPort;
    private final int hostPort;

    public PortMap(int containerPort, int hostPort) {
        this.containerPort = containerPort;
        this.hostPort = hostPort;
    }

    public static PortMap of(int containerPort, int hostPort) {
        return new PortMap(containerPort, hostPort);
    }

    public int localhostPort() {
        return hostPort;
    }

    public int containerPort() {
        return containerPort;
    }
}
