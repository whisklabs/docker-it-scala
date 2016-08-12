package com.whisk.docker

case class VolumeMapping(host: String, container: String, rw: Boolean = false)

case class ContainerLink(container: DockerContainer, alias: String) {
  require(container.name.nonEmpty, "Container must have a name")
}

case class DockerContainer(image: String,
                           name: Option[String] = None,
                           command: Option[Seq[String]] = None,
                           bindPorts: Map[Int, Option[Int]] = Map.empty,
                           tty: Boolean = false,
                           stdinOpen: Boolean = false,
                           links: Seq[ContainerLink] = Seq.empty,
                           env: Seq[String] = Seq.empty,
                           readyChecker: DockerReadyChecker = DockerReadyChecker.Always,
                           volumeMappings: Seq[VolumeMapping] = Seq.empty) {

  def withCommand(cmd: String*) = copy(command = Some(cmd))

  def withPorts(ps: (Int, Option[Int])*) = copy(bindPorts = ps.toMap)

  def withLinks(links: ContainerLink*) = copy(links = links.toSeq)

  def withReadyChecker(checker: DockerReadyChecker) = copy(readyChecker = checker)

  def withEnv(env: String*) = copy(env = env)

  def withVolumes(volumeMappings: Seq[VolumeMapping]) = copy(volumeMappings = volumeMappings)
}
