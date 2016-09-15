package com.whisk.docker

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.specs2.DockerTestKit

trait DockerTestKitDockerJava extends DockerTestKit with DockerKitDockerJava {}
