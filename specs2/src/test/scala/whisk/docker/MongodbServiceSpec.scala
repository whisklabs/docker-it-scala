package whisk.docker

import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._

class MongodbServiceSpec(env: Env) extends Specification
    with DockerTestKit
    with DockerMongodbService {

  implicit val ee = env.executionEnv

  def is = s2"""
  The mongodb container should be ready $x1
                                        """

  def x1 = mongodbContainer.isReady() must beTrue.await
}
