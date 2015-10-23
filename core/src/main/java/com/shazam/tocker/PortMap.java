/*
 * Copyright (C) 2011 Google Inc.
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
package com.shazam.tocker;

public class PortMap {
    private final int containerPort;
    private final int hostPort;

    private PortMap(int containerPort, int hostPort) {
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
