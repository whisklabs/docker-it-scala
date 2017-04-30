package com.whisk.docker

case class VolumeMapping(host: String, container: String, rw: Boolean = false)

case class ContainerLink(container: DockerContainer, alias: String) {
  require(container.name.nonEmpty, "Container must have a name")
}

case class LogLineReceiver(withErr: Boolean, f: String => Unit)

case class DockerPortMapping(hostPort: Option[Int] = None, address: String = "0.0.0.0")

case class DockerContainer(image: String,
                           name: Option[String] = None,
                           command: Option[Seq[String]] = None,
                           bindPorts: Map[Int, DockerPortMapping] = Map.empty,
                           tty: Boolean = false,
                           stdinOpen: Boolean = false,
                           links: Seq[ContainerLink] = Seq.empty,
                           unlinkedDependencies: Seq[DockerContainer] = Seq.empty,
                           env: Seq[String] = Seq.empty,
                           networkMode: Option[String] = None,
                           readyChecker: DockerReadyChecker = DockerReadyChecker.Always,
                           volumeMappings: Seq[VolumeMapping] = Seq.empty,
                           logLineReceiver: Option[LogLineReceiver] = None,
                           user: Option[String] = None,
                           hostname: Option[String] = None) {

  def withCommand(cmd: String*) = copy(command = Some(cmd))

  def withPorts(ps: (Int, Option[Int])*) =
    copy(bindPorts = ps.toMap.mapValues(hostPort => DockerPortMapping(hostPort)))

  def withPortMapping(ps: (Int, DockerPortMapping)*) = copy(bindPorts = ps.toMap)

  def withLinks(links: ContainerLink*) = copy(links = links.toSeq)

  def withUnlinkedDependencies(unlinkedDependencies: DockerContainer*) =
    copy(unlinkedDependencies = unlinkedDependencies.toSeq)

  def dependencies: Seq[DockerContainer] = links.map(_.container) ++ unlinkedDependencies

  def withReadyChecker(checker: DockerReadyChecker) = copy(readyChecker = checker)

  def withEnv(env: String*) = copy(env = env)

  def withNetworkMode(networkMode: String) = copy(networkMode = Some(networkMode))

  def withVolumes(volumeMappings: Seq[VolumeMapping]) = copy(volumeMappings = volumeMappings)

  def withLogLineReceiver(logLineReceiver: LogLineReceiver) =
    copy(logLineReceiver = Some(logLineReceiver))

  def withUser(user: String) = copy(user = Some(user))

  def withHostname(hostname: String) = copy(hostname = Some(hostname))
}
