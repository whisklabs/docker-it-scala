package whisk.docker.test.specs2

import org.specs2.specification.core.SpecificationStructure
import org.specs2.specification.create.FragmentsFactory
import org.specs2.specification.core.Fragments

trait BeforeAfterAllStopOnError extends SpecificationStructure
    with FragmentsFactory {
  def beforeAll
  def afterAll
  override def map(fs: => Fragments) =
    super.map(fs).prepend(
      fragmentFactory.step(beforeAll).stopOnError
    ).append(fragmentFactory.step(afterAll))
}
