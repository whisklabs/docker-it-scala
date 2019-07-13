lazy val commonSettings = Seq(
  organization := "com.whisk",
  version := "0.10.0-beta7",
  scalaVersion := "2.13.0",
  crossScalaVersions := Seq("2.13.0", "2.12.8", "2.11.12"),
  scalacOptions ++= Seq("-feature", "-deprecation"),
  fork in Test := true,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  sonatypeProfileName := "com.whisk",
  publishMavenStyle := true,
  publishTo := Some(Opts.resolver.sonatypeStaging),
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
  project
    .in(file("."))
    .settings(commonSettings: _*)
    .settings(publish := {}, publishLocal := {}, packagedArtifacts := Map.empty)
    .aggregate(core, scalatest, samples, coreShaded)

lazy val core =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-core",
      libraryDependencies ++= Seq(
        "org.slf4j" % "slf4j-api" % "1.7.25",
        "com.spotify" % "docker-client" % "8.15.2",
        "com.google.code.findbugs" % "jsr305" % "3.0.1",
      )
    )

lazy val scalatest =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-scalatest",
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "3.0.8",
        "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"
      )
    )
    .dependsOn(core)

lazy val samples =
  project
    .settings(commonSettings: _*)
    .settings(name := "docker-testkit-samples")
    .dependsOn(core, scalatest)

lazy val tests =
  project
    .settings(commonSettings: _*)
    .settings(
      name := "docker-testkit-tests",
      libraryDependencies ++= Seq(
        "org.postgresql" % "postgresql" % "42.1.4" % "test",
        "mysql" % "mysql-connector-java" % "5.1.44" % "test"
      )
    )
    .dependsOn(core, scalatest, samples % "test")


lazy val coreShaded =
  project
    .in(file("core"))
    .settings(commonSettings: _*)
    .settings(name := "docker-testkit-core-shaded",
      libraryDependencies ++=
        Seq("com.spotify" % "docker-client" % "8.15.2" classifier "shaded",
          "com.google.code.findbugs" % "jsr305" % "3.0.1"),
      target := baseDirectory.value / "target-shaded"
    )