docker-it-scala
=============

[![CI](https://github.com/whisklabs/docker-it-scala/actions/workflows/ci.yaml/badge.svg)](https://github.com/whisklabs/docker-it-scala/actions/workflows/ci.yaml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.whisk/docker-testkit-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.whisk/docker-testkit-core_2.12)
[![Join the chat at https://gitter.im/whisklabs/docker-it-scala](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/whisklabs/docker-it-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Set of utility classes to make integration testing with dockerised services in Scala easy.

You can read about reasoning behind it at [Finely Distributed](https://finelydistributed.io/integration-testing-with-docker-and-scala-85659d037740#.8mbrg311p).

## Setup

docker-it-scala works with Spotify's docker-client to communicate to docker engine through *REST API* or *unix socket*.
- [Spotify's docker-client](https://github.com/spotify/docker-client) (used in Whisk)

```scala
libraryDependencies ++= Seq(
  "com.whisk" %% "docker-testkit-scalatest" % "0.11.0" % "test"
```

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

- [Elasticsearch](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/testkit/DockerElasticsearchService.scala)
- [Mongodb](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/testkit/DockerMongodbService.scala)
- [MySQL](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/testkit/DockerMysqlService.scala)
- [Postgres](https://github.com/whisklabs/docker-it-scala/blob/master/samples/src/main/scala/com/whisk/docker/testkit/DockerPostgresService.scala)
- [Multi container test](https://github.com/whisklabs/docker-it-scala/blob/master/tests/src/test/scala/com/whisk/docker/testkit/test/MultiContainerTest.scala)

# Defining Containers

There are two ways to define a docker container.

Code based definitions and via `typesafe-config`.

## Code based definitions

```scala
import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite

trait DockerMongodbService extends DockerTestKitForAll {
  self: Suite =>

  val DefaultMongodbPort = 27017

  val mongodbContainer = ContainerSpec("mongo:3.4.8")
    .withExposedPorts(DefaultMongodbPort)
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .toContainer

  override val managedContainers: ManagedContainers = mongodbContainer.toManagedContainer
}
```

You can check [usage example](https://github.com/whisklabs/docker-it-scala/blob/master/tests/src/test/scala/com/whisk/docker/testkit/test/MongodbServiceTest.scala)

### Container Paths

- Elasticsearch => `docker.elasticsearch`
- Mongodb => `docker.mongo`
- Neo4j => `docker.mysql`
- Postgres => `docker.postgres`

### Fields

- `image-name` required  (String)
- `container-name` optional (String)
- `command` optional (Array of Strings)
- `entrypoint` optional (Array of Strings)
- `environmental-variables` optional (Array of Strings)
- `ready-checker` optional structure
  - `log-line` optional (String)
  - `http-response-code`
    - `code` optional (Int - defaults to `200`)
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
- `memory` optional (Long)
- `memory-reservation` optional (Long)

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
class MultiContainerTest
  extends AnyFunSuite
    with DockerElasticsearchService
    with DockerMongodbService {

  override val managedContainers: ContainerGroup =
    ContainerGroup.of(elasticsearchContainer, mongodbContainer)

  test("both containers should be ready") {
    assert(
      elasticsearchContainer.state().isInstanceOf[ContainerState.Ready],
      "elasticsearch container is ready"
    )
    assert(elasticsearchContainer.mappedPortOpt(9200).nonEmpty, "elasticsearch port is exposed")

    assert(mongodbContainer.state().isInstanceOf[ContainerState.Ready], "mongodb is ready")
    assert(mongodbContainer.mappedPortOpt(27017).nonEmpty, "port 2017 is exposed")
  }
}
```