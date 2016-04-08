import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._

class SampleServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with SampleService {
  override def testConfigSource = "akka.loglevel = WARNING"
  val repo = new PersonRepository

  "Service" should "respond to single IP query" in {
    Get("/") ~> appRoutes ~> check {
      status shouldBe OK
      responseAs[String] shouldBe "witam"
    }
  }

  it should "consume Person and Respond with Response" in {
    Post(s"/api/test", Person("john", 30)) ~> appRoutes ~> check {
      status shouldBe OK
      responseAs[Response] shouldBe Response(31)
    }
  }
}
