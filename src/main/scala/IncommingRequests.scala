import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.language.postfixOps

object IncommingRequests extends App {
  implicit val system = ActorSystem("in-test")
  implicit val materializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global
  var counter = 0
  val routes: Route = logRequestResult("foo-service") {
    pathPrefix("foo") {
      pathEndOrSingleSlash {
        put {
          entity(as[Foo]) { foo =>
            complete{
              counter += 1
              if (counter >= 199999)
                println(s"==== GOT $counter requests")
              Future(foo).map(r => s"Update - $r ")
            }
          }
        }
      }
    }
  }
  Http().bindAndHandle(routes, "0.0.0.0", 9000)
}
