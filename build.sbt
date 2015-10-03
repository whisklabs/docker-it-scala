
lazy val commonSettings = Seq(
  organization := "com.whisk",
  version := "0.2.1",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.11.7", "2.10.5"),
  scalacOptions ++= Seq("-feature", "-deprecation"),
  fork in Test := true,
  bintrayOrganization := Some("whisk"),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayRepository := "maven"
)

lazy val root =
  project.in(file("."))
    .settings(commonSettings: _*)
    .settings(
      publish := {},
      publishLocal := {})
    .aggregate(core, config, scalatest, specs2)

lazy val core =
  project
    .settings(commonSettings: _*)
    .settings(
    name := "docker-it-scala-core",
    resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven",
    libraryDependencies ++=
      Seq(
        "me.lessis" %% "undelay" % "0.1.0",
        "me.lessis" %% "odelay-core" % "0.1.0",
        "com.github.docker-java" % "docker-java" % "1.4.0"))

lazy val scalatest =
  project
    .settings(commonSettings: _*)
    .settings(
    name := "docker-testkit-scalatest",
      libraryDependencies ++=
        Seq(
          "org.scalatest" %% "scalatest" % "2.2.4",
          "ch.qos.logback" % "logback-classic" % "1.1.2" % "test"))
    .dependsOn(core, config % "test->test")

lazy val specs2 =
  project
    .settings(commonSettings: _*)
    .settings(
    name := "docker-testkit-specs2",
      libraryDependencies ++=
        Seq(
          "org.specs2" %% "specs2-core" % "3.6.4",
          "ch.qos.logback" % "logback-classic" % "1.1.2" % "test"))
    .dependsOn(core, config % "test->test")

lazy val config =
  project
    .settings(commonSettings: _*)
    .settings(
    name := "docker-testkit-config",
      libraryDependencies ++=
        Seq(
          "net.ceedubs" %% "ficus" % "1.1.2"))
    .dependsOn(core)
