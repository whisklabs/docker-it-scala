package whisk.docker.config

import org.specs2.Specification
import org.specs2.specification.core.Env
import whisk.docker.{DockerTestKit, DockerRabbitMqService}

class RabbitMqServiceSpec(env: Env) extends Specification
 with DockerTestKit
 with DockerRabbitMqService {

   implicit val ee = env.executionEnv

   def is = s2"""
   The rabbit-mq container should be ready $x1
                                         """

   def x1 = rabbitMqContainer.isReady() must beTrue.await
 }