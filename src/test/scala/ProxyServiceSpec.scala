import java.net.URI

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest.{FlatSpec, Matchers}
import spray.json.DefaultJsonProtocol

case class ProxyResponse(method: String, path: String, query: Option[String])

class ProxyServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with ProxyService with DefaultJsonProtocol with SprayJsonSupport {
    override val proxies: Map[String, URI] = Map("service" -> new URI("http://localhost:9000/mocked"))

    implicit val ProxyReponseFormat = jsonFormat3(ProxyResponse)

    override def flow(uri: URI): Flow[HttpRequest, HttpResponse, Any] =
        Flow[HttpRequest].map {
            request => HttpResponse(status = StatusCodes.OK, entity = marshal(
                ProxyResponse(request.method.value, path = request.uri.path.toString, query = request.uri.rawQueryString)
            ))
        }

    "ProxyService" should "respond to single IP query" in {
        Get("/service/part1/part2?q=1") ~> proxyRoutes ~> check {
            status shouldBe StatusCodes.OK
            responseAs[ProxyResponse] shouldBe ProxyResponse("GET", "/part1/part2", Some("q=1"))
        }
    }
}
