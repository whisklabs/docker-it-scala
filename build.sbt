organization := "com.whisk"

name := "docker-it-scala"

version := "0.2.0"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7", "2.10.5")

scalariformSettings

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

fork in Test := true

val gitHeadCommitSha = settingKey[String]("current git commit SHA")

gitHeadCommitSha in ThisBuild := Process("git rev-parse --short HEAD").lines.head

libraryDependencies ++= Seq(
  "com.github.docker-java" % "docker-java" % "1.4.0",
  "me.lessis" %% "odelay-core" % "0.1.0",
  "me.lessis" %% "undelay" % "0.1.0",
  "org.scalatest" %% "scalatest" % "2.2.4",
  "ch.qos.logback" % "logback-classic" % "1.1.2" % "test")

publishTo := {
  val dir = if (version.value.trim.endsWith(gitHeadCommitSha.value)) "snapshots" else "releases"
  val repo = Path.userHome / "mvn-repo" / dir
  Some(Resolver.file("file", repo) )
}