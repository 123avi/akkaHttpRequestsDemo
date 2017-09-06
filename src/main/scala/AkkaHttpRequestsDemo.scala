import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContextExecutor, Future}

case class User(userId: Int, id: Int , title: String, body: String)

trait JsonFormats extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat4(User.apply)
}

trait Service extends JsonFormats {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

//  val host = "jsonplaceholder.typicode.com"
//  val port = 443

  val host = "localhost"
  val port = 9000



  def apiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(host, port)
//    Http().outgoingConnectionHttps(host, port)

  def apiRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(apiConnectionFlow).runWith(Sink.head)

//  def fetchUserInfo(userId: String): Future[Set[String]] = {
//    apiRequest(RequestBuilding.Get(s"/posts/$userId")).flatMap { response =>
//      response.status match {
//        case OK => Unmarshal(response.entity).to[Set[String]]
//        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
//          val err = s"Request failed with status code ${response.status} and entity $entity"
//          logger.error(err)
//          Future.failed(new IOException(err))
//        }
//      }
//    }
//  }


}

object AkkaHttpRequestsDemo extends App with Service {

  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)
    val numRequests = 200000
  for (i <- 1 to numRequests){
    apiRequest(RequestBuilding.Put("/foo", Foo(i)))

  }
  println(s"===== done sending $numRequests requests")

}
