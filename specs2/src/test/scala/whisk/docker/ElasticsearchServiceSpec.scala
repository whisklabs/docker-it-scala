package whisk.docker

import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._

class ElasticsearchServiceSpec(env: Env) extends Specification
    with DockerElasticsearchService
    with DockerTestKit {

  implicit val ee = env.executionEnv

  def is = s2"""
  The elasticsearch container should be ready $x1
                                              """

  def x1 = elasticsearchContainer.isReady() must beTrue.await
}
