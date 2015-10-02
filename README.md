docker-it-scala
=============

Set of utility classes to make integration testing with dockerised services in Scala easy.

You can read about reasoning behind it on http://finelydistributed.io/integration-tests-with-docker/

## Dependency

    resolvers += "Whisk" at "https://dl.bintray.com/whisk/maven"

    libraryDependencies += "com.whisk" %% "docker-it-scala" % "0.2.0"

## Example

Container definition

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

Using in ScalaTest:

```scala
class MyMongoSpec extends FlatSpec with Matchers with DockerMongodbService {
  ...
}
```

Multiple containers:

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
