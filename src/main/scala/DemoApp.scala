import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}

case class Person(name: String, age: Int)

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val personFormat = jsonFormat2(Person)
}

class PersonRepository {
    private val store = scala.collection.mutable.Map[String, Person]()

    def save(person: Person): Future[Option[Person]] = {
        if (person.name == "err") {
            Future.failed(new RuntimeException("bad name"))
        } else {
            Future.successful(store.put(person.name, person))
        }
    }

    def find(id: String): Future[Option[Person]] = {
        if(id =="err") {
            Future.failed(new RuntimeException("somerror"))
        } else if (id =="maciej") {
            Future.successful(Some(Person("maciej", 12)))
        } else {
            Future.successful(store.get(id))
        }
    }
}

trait SampleService extends JsonSupport {
    implicit def executor: ExecutionContextExecutor

    def appRoutes(repo: PersonRepository): Route =
        post {
            entity(as[Person]) { person =>
                complete {
                    repo.save(person)
                }
            }
        } ~
        pathPrefix(Segment) { id =>
            get {
                val futurePerson = repo.find(id)
                rejectEmptyResponse(
                    complete(futurePerson)
                )
            }
        }
}

object DemoApp extends App with SampleService {

    implicit val system = ActorSystem()
    implicit val executor: ExecutionContextExecutor = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val repo = new PersonRepository

    val routes = path("") {
        complete("service2")
    } ~ pathPrefix("api" / "person") {
        appRoutes(repo)
    }

    Http().bindAndHandle(routes, "localhost", 9001)
}
