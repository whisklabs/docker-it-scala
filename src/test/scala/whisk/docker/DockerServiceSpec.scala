package whisk.docker

import org.scalatest.concurrent.ScalaFutures
import org.scalatest._

/**
 * @author alari
 * @since 11/20/14
 */
class DockerServiceSpec extends FeatureSpec with Matchers with BeforeAndAfterAll with GivenWhenThen
    with ScalaFutures {

  override def beforeAll(): Unit = {
    println("before all")
  }

  feature("docker service") {
    scenario("docker service builder created") {
      1 shouldEqual 2
    }
  }

  override def afterAll(): Unit = {
    println("after all")
  }

}
