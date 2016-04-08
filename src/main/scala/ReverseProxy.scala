import java.net.URI

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

trait ProxyService {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: ActorMaterializer

    val proxies: Map[String, URI]

    def flow(uri: URI): Flow[HttpRequest, HttpResponse, Any] =
        Http(system).outgoingConnection(uri.getHost, uri.getPort)

    def proxyTo(context: RequestContext, flow: Flow[HttpRequest, HttpResponse, Any]): Future[RouteResult] = {
        val query = context.request.uri.rawQueryString.map("?"+_).getOrElse("")

        val requestUri = Uri(context.unmatchedPath.toString() + query)

        val proxyRequest = context.request.copy(uri = requestUri)

        Source.single(proxyRequest)
            .via(flow)
            .runWith(Sink.head)
            .flatMap(context.complete(_))
    }

    def proxyRoutes(proxies: Map[String, URI]) = pathPrefix(Segment) {
        serviceName =>
            proxies.get(serviceName) match {
                case Some(uri) =>
                    (context: RequestContext) => proxyTo(context, flow(uri))
                case None => reject
            }
    }
}

object ReverseProxy extends App with ProxyService {
    override implicit val system = ActorSystem()
    override implicit val executor = system.dispatcher
    override implicit val materializer = ActorMaterializer()

    val config = ConfigFactory.load()
    import scala.collection.JavaConverters._

    val proxies: Map[String, URI] = config.getConfig("proxy").entrySet().asScala
        .map(entry => entry.getKey -> new URI(entry.getValue.unwrapped().toString)).toMap


    val appRoutes = proxyRoutes(proxies)

    Http().bindAndHandle(appRoutes, config.getString("http.interface"), config.getInt("http.port"))
}
