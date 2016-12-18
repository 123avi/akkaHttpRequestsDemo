import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.IOException
import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json.DefaultJsonProtocol

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

  val host = "jsonplaceholder.typicode.com"
  val port = 443

  lazy val apiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnectionHttps(host, port)

  def apiRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(apiConnectionFlow).runWith(Sink.head)

  def fetchUserInfo(userId: String): Future[User] = {
    apiRequest(RequestBuilding.Get(s"/posts/$userId")).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[User]
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val err = s"Request failed with status code ${response.status} and entity $entity"
          logger.error(err)
          Future.failed(new IOException(err))
        }
      }
    }
  }

  val routes = {
    logRequestResult("akka-http-microservice") {
      pathPrefix("users") {
        (get & path(Segment)) { userId =>
          complete {
            fetchUserInfo(userId).recover{ case e =>
                BadRequest -> e.getMessage

            }.map[ToResponseMarshallable] {
              case u:User=> u
              case error => BadRequest -> s"Failed $error"
            }
          }
        }
      }
    }
  }
}

object AkkaHttpRequestsDemo extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
