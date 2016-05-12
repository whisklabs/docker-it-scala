package com.whisk.docker

case class DockerContainer(
                            image: String,
                            command: Option[Seq[String]] = None,
                            bindPorts: Map[Int, Option[Int]] = Map.empty,
                            tty: Boolean = false,
                            stdinOpen: Boolean = false,
                            links: Map[DockerContainer, String] = Map.empty,
                            env: Seq[String] = Seq.empty,
                            readyChecker: DockerReadyChecker = DockerReadyChecker.Always) {

  def withCommand(cmd: String*) = copy(command = Some(cmd))

  def withPorts(ps: (Int, Option[Int])*) = copy(bindPorts = ps.toMap)

  def withLinks(links: (DockerContainer, String)*) = copy(links = links.toMap)

  def withReadyChecker(checker: DockerReadyChecker) = copy(readyChecker = checker)

  def withEnv(env: String*) = copy(env = env)
}
