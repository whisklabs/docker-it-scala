docker-it-scala
=============

[![Join the chat at https://gitter.im/whisklabs/docker-it-scala](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/whisklabs/docker-it-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Set of utility classes to make integration testing with dockerised services in Scala easy.

You can read about reasoning behind it at [Finely Distributed](https://finelydistributed.io/integration-testing-with-docker-and-scala-85659d037740#.8mbrg311p).

## Setup

docker-it-scala utilises [docker-java](https://github.com/docker-java/docker-java)'s way of configuration in performing
initialisation with default settings.

```scala
import com.github.dockerjava.core.DockerClientConfig

trait DockerKit {
  implicit val docker: Docker =
    new Docker(DockerClientConfig.createDefaultConfigBuilder().build())

  ...
}
```

That makes it possible to configure it through enviroment variables

### docker-machine setup

Docker Toolbox 1.9 (maybe also before, came some time in 2015) contains the [`docker-machine`](https://docs.docker.com/machine/) command. With it, you get going simply by:

```
docker-machine start default
eval $(docker-machine env default)
```

<!-- I like having '$' in shell commands shown in documentation. However, left them out here for consistency with the rest of the README. AKa010216 -->

### Boot2docker setup

```
export DOCKER_HOST=tcp://192.168.59.103:2376
export DOCKER_CERT_PATH=/Users/<username>/.boot2docker/certs/boot2docker-vm
export DOCKER_TLS_VERIFY=1
```

### Setup without SSL

```
export DOCKER_HOST=tcp://127.0.0.1:2375
```

### Docker for Mac setup

Since version `0.9.0-M2` you can use implementation with Spotify's [docker-client](https://github.com/spotify/docker-client) in Docker for Mac setup as it works better with unix socket
 
```scala
import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.{DockerFactory, DockerKit}

trait MyDockerKit extends DockerKit {
  override implicit val dockerFactory: DockerFactory =
    new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())
}
```

with

```
export DOCKER_HOST=unix:///var/run/docker.sock
```

## Dependency

Artifacts are available for Scala 2.10 and 2.11

Include a dependency on one of the testkits of your choice to get started.

```scala
libraryDependencies += "com.whisk" %% "docker-testkit-specs2" % "0.8.3" % "test"
```

```scala
libraryDependencies += "com.whisk" %% "docker-testkit-scalatest" % "0.8.3" % "test"
```

If you want to configure via typesafe config (only for Scala 2.11), also include

```scala
libraryDependencies += "com.whisk" %% "docker-testkit-config" % "0.8.3" % "test"
```


### Unstable dependencies

```scala
libraryDependencies ++= Seq(
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.0-M3" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.0-M3" % "test")
```

# Sample Services

- [Cassandra](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/DockerCassandraService.scala)
- [Elasticsearch](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/DockerElasticsearchService.scala)
- [Kafka](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/DockerKafkaService.scala)
- [Mongodb](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/DockerMongodbService.scala)
- [Neo4j](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/DockerNeo4jService.scala)
- [Postgres](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/DockerPostgresService.scala)

# Defining Containers

There are two ways to define a docker container.

Code based definitions and via `typesafe-config`.

## Code based definitions

```scala
trait DockerMongodbService extends DockerKit {

  val DefaultMongodbPort = 27017

  val mongodbContainer = DockerContainer("mongo:3.0.6")
    .withPorts(DefaultMongodbPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] = mongodbContainer :: super.dockerContainers
}
```

## Typesafe Configuration

`docker-testkit-config` enables you to use a typesafe config to
define your docker containers. Just put an `application.conf` file in
your classpath.

The container definitions are nested in the structure of name `docker`

```conf
docker {
...
...
}
```

See
[application.conf](https://github.com/whisklabs/docker-it-scala/blob/master/config/src/test/resources/application.conf)
for more examples.

Usage in code

```scala
trait DockerMongodbService extends DockerKitConfig {

  val mongodbContainer = configureDockerContainer("docker.mongodb")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}

```

### Container Paths

- Cassandra => `docker.cassandra`
- Elasticsearch => `docker.elasticsearch`
- Kafka => `docker.kafka`
- Mongodb => `docker.mongo`
- Neo4j => `docker.neo4j`
- Postgres => `docker.postgres`

### Fields

- `image-name` required  (String)
- `environmental-variables` optional (Array of Strings)
- `ready-checker` optional structure
  - `log-line` optional (String)
  - `http-response-code`
    - `port` required (Int)
	- `path` optional (String - defaults to `/`)
	- `within` optional (Int)
	- `looped` optional structure
      - `attempts` required (Int)
      - `delay` required (Int)
- `port-maps` optional structure (list of structures)
  - `SOME_MAPPING_NAME`
    - `internal` required (Int)
    - `external` optional (Int)
- `volume-maps` optional structure (list of structures)
  - `container` required (String)
  - `host`      required (String)
  - `rw`        optional (Boolean - default:false)

# Testkit

There are two testkits available -- one for `scalatest` and one for
`specs2`.

Both set up the necessary docker containers and check that they are
ready **BEFORE** any test is run, and doesn't close the container
until **ALL** the tests are run.


## Using in ScalaTest:

```scala
class MyMongoSpec extends FlatSpec with Matchers with DockerMongodbService {
  ...
}
```

### With Multiple containers:

```scala
class AllAtOnceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerElasticsearchService with DockerCassandraService with DockerNeo4jService with DockerMongodbService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "all containers" should "be ready at the same time" in {
    dockerContainers.map(_.image).foreach(println)
    dockerContainers.forall(_.isReady().futureValue) shouldBe true
  }
}
```

## Using in Specs2

Examples can be found in
[the specs2 module's tests](https://github.com/whisklabs/docker-it-scala/tree/master/specs2/src/test/scala/com/whisk/docker)
