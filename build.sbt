organization := "com.whisk"

name := "docker-it-scala"

version := "0.2.0"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7", "2.10.5")

bintrayOrganization := Some("whisk")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayRepository := "maven"

scalariformSettings

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

fork in Test := true

libraryDependencies ++= Seq(
  "com.github.docker-java" % "docker-java" % "1.4.0",
  "me.lessis" %% "odelay-core" % "0.1.0",
  "me.lessis" %% "undelay" % "0.1.0",
  "org.scalatest" %% "scalatest" % "2.2.4",
  "org.specs2" %% "specs2-core" % "3.6.4",
  "ch.qos.logback" % "logback-classic" % "1.1.2" % "test")
