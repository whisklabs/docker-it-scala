docker-it-scala
=============

Set of utility classes to make integration testing with dockerised services in Scala easy.

You can read about reasoning behind it on http://finelydistributed.io/integration-tests-with-docker/

## Dependency

Artifacts are available for Scala 2.10 and 2.11

You'll need to add a resolver

    resolvers += "Whisk" at "https://dl.bintray.com/whisk/maven"

Include a dependency on one of the testkits of your choice to get started.

```scala
libraryDependencies += "com.whisk" %% "docker-testkit-specs2" % "0.2.1"
```

```scala
libraryDependencies += "com.whisk" %% "docker-testkit-scalatest" % "0.2.1"
```

If you want to configure via typesafe config, also include

```scala
libraryDependencies += "com.whisk" %% "docker-testkit-config" % "0.2.1"
```

# Available Services

- Cassandra
- Elasticsearch
- Kafka
- Mongodb
- Neo4j
- Postgres

Additional contributions are welcome, and since these are just traits,
you can always roll your own!

# Defining Containers

There are two ways to define a docker container.

Code based definitions and via `typesafe-config`.

## Code based definitions

```scala
trait DockerMongodbService extends DockerKit {

  val DefaultMongodbPort = 27017

  val mongodbContainer = DockerContainer("mongo:3.0.6")
    .withPorts(DefaultMongodbPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("waiting for connections on port")))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] = mongodbContainer :: super.dockerContainers
}
```

## Typesafe Configuration

`docker-testkit-config` enables you to use a typsesafe config to
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

See [this folder](https://github.com/whisklabs/docker-it-scala/tree/master/config/src/main/scala/com/whisk/docker/config) for more services with config examples.

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
[the specs2 module's tests](https://github.com/AdAgility/docker-it-scala/blob/docker-test-kit/specs2/src/test/)
