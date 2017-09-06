import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Foo (i: Int)

object Foo extends DefaultJsonProtocol with SprayJsonSupport{
  implicit val ftoj = jsonFormat1(Foo.apply)
}
