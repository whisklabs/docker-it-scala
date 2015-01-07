package whisk.docker

import org.scalatest.Suite
import whisk.docker.test.DockerTestKit

import scala.concurrent.{ Future, ExecutionContext }
import com.github.dockerjava.core.DockerClientConfig

trait DockerKit {
  implicit val docker: Docker = new Docker(DockerClientConfig.createDefaultConfigBuilder().build())

  {
    this match {
      case _: Suite =>
        this match {
          case _: DockerTestKit =>
          case _ =>
            System.err.println("!!!\n You're using DockerKit inside scalatest, but forgot to mix in DockerTestKit. Please do it to avoid strangeness. \n !!!")
        }
    }
  }

  // we need ExecutionContext in order to run docker.init() / docker.stop() there
  implicit def dockerExecutionContext: ExecutionContext = ExecutionContext.global

  def dockerContainers: List[DockerContainer] = Nil

  def stopRmAll(): Future[Seq[DockerContainer]] =
    Future.traverse(dockerContainers)(_.remove(force = true))

  def pullImages(): Future[Seq[DockerContainer]] =
    Future.traverse(dockerContainers)(_.pull())

  def initReadyAll(): Future[Seq[(DockerContainer, Boolean)]] =
    Future.traverse(dockerContainers)(_.init()).flatMap(Future.traverse(_)(c => c.isReady().map(c -> _).recover {
      case e =>
        System.err.println(e.getMessage)
        e.printStackTrace(System.err)
        c -> false
    }))

  protected def logException(e: Throwable): Unit = {
    System.err.println(e)
    e.printStackTrace(System.err)
  }
}
