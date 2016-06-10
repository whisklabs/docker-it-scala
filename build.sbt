
lazy val commonSettings = Seq(
  organization := "com.whisk",
  version := "0.8.1",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.11.7", "2.10.5"),
  scalacOptions ++= Seq("-feature", "-deprecation"),
  fork in Test := true,
  licenses +=("MIT", url("http://opensource.org/licenses/MIT")),
  sonatypeProfileName := "com.whisk",
  pomExtra in Global := {
    <url>https://github.com/whisklabs/docker-it-scala</url>
      <scm>
        <connection>scm:git:github.com/whisklabs/docker-it-scala.git</connection>
        <developerConnection>scm:git:git@github.com:whisklabs/docker-it-scala.git</developerConnection>
        <url>github.com/whisklabs/docker-it-scala.git</url>
      </scm>
      <developers>
        <developer>
          <id>viktortnk</id>
          <name>Viktor Taranenko</name>
          <url>https://github.com/viktortnk</url>
        </developer>
        <developer>
          <id>alari</id>
          <name>Dmitry Kurinskiy</name>
          <url>https://github.com/alari</url>
        </developer>
      </developers>
  }
)

lazy val root =
  project.in(file("."))
    .settings(commonSettings: _*)
    .settings(
      publish := {},
      publishLocal := {},
      packagedArtifacts := Map.empty)
    .aggregate(core, config, scalatest, specs2)

lazy val core =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-core",
      libraryDependencies ++=
        Seq("com.github.docker-java" % "docker-java" % "3.0.0",
          "com.google.code.findbugs" % "jsr305" % "3.0.1"))

lazy val samples =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-samples")
    .dependsOn(core)

lazy val scalatest =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-scalatest",
      libraryDependencies ++=
        Seq(
          "org.scalatest" %% "scalatest" % "2.2.6",
          "ch.qos.logback" % "logback-classic" % "1.1.5" % "test"))
    .dependsOn(core, samples % "test")

lazy val specs2 =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-specs2",
      libraryDependencies ++=
        Seq(
          "org.specs2" %% "specs2-core" % "3.6.4",
          "ch.qos.logback" % "logback-classic" % "1.1.5" % "test"))
    .dependsOn(core, samples % "test")

lazy val config =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-config",
      libraryDependencies ++=
        Seq(
          "net.ceedubs" %% "ficus" % "1.1.2",
          "org.scalatest" %% "scalatest" % "2.2.6" % "test"),
      publish := scalaVersion map {
        case x if x.startsWith("2.10") => {}
        case _ => publish.value
      }
    )
    .dependsOn(core)
