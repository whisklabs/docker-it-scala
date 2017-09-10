docker-it-scala
=============

[![Build Status](https://travis-ci.org/whisklabs/docker-it-scala.svg?branch=master)](https://travis-ci.org/whisklabs/docker-it-scala)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.whisk/docker-testkit-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.whisk/docker-testkit-core_2.12)
[![Join the chat at https://gitter.im/whisklabs/docker-it-scala](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/whisklabs/docker-it-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Set of utility classes to make integration testing with dockerised services in Scala easy.

You can read about reasoning behind it at [Finely Distributed](https://finelydistributed.io/integration-testing-with-docker-and-scala-85659d037740#.8mbrg311p).

## Setup

docker-it-scala can work with two underlying libraries to communicate to docker engine through *REST API* or *unix socket*.
- [Spotify's docker-client](https://github.com/spotify/docker-client) (used in Whisk)
- [docker-java](https://github.com/docker-java/docker-java)

*Note: there is no specific recommendation to use one of them, over the other, but we hear people using Spotify's one more often, so you might get better support for it*.

There are separate artifacts available for these libraries:

**Spotify's docker-client**

```scala
libraryDependencies ++= Seq(
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.4" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.4" % "test")
```

**docker-java**

```scala
libraryDependencies ++= Seq(
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.4" % "test",
  "com.whisk" %% "docker-testkit-impl-docker-java" % "0.9.4" % "test")
```

You don't necessarily have to use `scalatest` dependency as demonstrated above.
You can create your custom bindings into your test environment, whether you use different initialisation technique or different framework.
Have a look at [this specific trait](https://github.com/whisklabs/docker-it-scala/blob/master/scalatest/src/main/scala/com/whisk/docker/scalatest/DockerTestKit.scala)


### Overriding execution environment

If you need to have custom setup for you environment, you need to override `dockerFactory` field,  providing `DockerClient` instance

```scala
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.whisk.docker.{DockerFactory, DockerKit}

trait MyCustomDockerKitSpotify extends DockerKit {

  private val client: DockerClient = DefaultDockerClient.fromEnv().build()

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(client)
}

```

Check [docker-client](https://github.com/spotify/docker-client) library project for configuration options.

### Configuration

You should be able to provide configuration purely through environment variables.

Examples:

```
export DOCKER_HOST=tcp://127.0.0.1:2375
```

```
export DOCKER_HOST=unix:///var/run/docker.sock
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
import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

trait DockerMongodbService extends DockerKit {

  val DefaultMongodbPort = 27017

  val mongodbContainer = DockerContainer("mongo:3.0.6")
    .withPorts(DefaultMongodbPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}
```

You can check [usage example](https://github.com/whisklabs/docker-it-scala/blob/master/scalatest/src/test/scala/com/whisk/docker/MongodbServiceSpec.scala)

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
