
lazy val commonSettings = Seq(
  organization := "com.whisk",
  version := "0.9.1",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.12.1", "2.11.8", "2.10.5"),
  scalacOptions ++= Seq("-feature", "-deprecation"),
  fork in Test := true,
  licenses +=("MIT", url("http://opensource.org/licenses/MIT")),
  sonatypeProfileName := "com.whisk",
  scalafmtConfig := Some(file(".scalafmt")),
  // TODO Remove once this workaround no longer needed (https://github.com/sbt/sbt/issues/2786):
  ivyScala := { ivyScala.value map {_.copy(overrideScalaVersion = sbtPlugin.value)} },
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
    .aggregate(core, testkitSpotifyImpl, testkitDockerJavaImpl, config, scalatest, specs2, samples)

lazy val core =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-core",
      libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21")

lazy val testkitSpotifyImpl =
  project.in(file("impl/spotify"))
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-impl-spotify",
      libraryDependencies ++=
        Seq("com.spotify" % "docker-client" % "7.0.2",
            "com.google.code.findbugs" % "jsr305" % "3.0.1"))
    .dependsOn(core)

lazy val testkitDockerJavaImpl =
  project.in(file("impl/docker-java"))
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-impl-docker-java",
      libraryDependencies ++=
        Seq("com.github.docker-java" % "docker-java" % "3.0.7",
            "com.google.code.findbugs" % "jsr305" % "3.0.1"))
    .dependsOn(core)

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
          "org.scalatest" %% "scalatest" % "3.0.1",
          "ch.qos.logback" % "logback-classic" % "1.1.7" % "test",
          "org.postgresql" % "postgresql" % "9.4.1210" % "test"))
    .dependsOn(core, testkitSpotifyImpl % "test", testkitDockerJavaImpl % "test", samples % "test")

lazy val specs2 =
   project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-specs2",
      libraryDependencies ++=
        Seq(
          "org.specs2" %% "specs2-core" % "3.8.6",
          "ch.qos.logback" % "logback-classic" % "1.1.7" % "test",
          "org.postgresql" % "postgresql" % "9.4.1210" % "test"))
    .dependsOn(core, samples % "test", testkitDockerJavaImpl % "test")

lazy val config =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-config",
      libraryDependencies ++=
        Seq(
          "com.iheart" %% "ficus" % "1.4.0",
          "org.scalatest" %% "scalatest" % "3.0.1" % "test"),
      publish := scalaVersion map {
        case x if x.startsWith("2.10") => {}
        case _ => publish.value
      }
    )
    .dependsOn(core, testkitDockerJavaImpl)
