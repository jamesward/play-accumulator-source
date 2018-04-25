import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalatest.{AsyncWordSpec, EitherValues, MustMatchers}
import play.api.http.{HeaderNames, HttpErrorHandler}
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import play.core.parsers.Multipart
import play.core.parsers.Multipart.FileInfo

import scala.concurrent.Future

class MultipartSpec extends AsyncWordSpec with EitherValues with MustMatchers {

  implicit lazy val actorSystem = ActorSystem()

  implicit lazy val materializer = ActorMaterializer()

  lazy val httpErrorHandler: HttpErrorHandler = new HttpErrorHandler {
    override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = Future.failed(new Exception())

    override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.failed(new Exception())
  }

  def handleFilePartAsByteString: Multipart.FilePartHandler[Source[ByteString, _]] = {
    case FileInfo(partName, filename, contentType) =>
      Accumulator.source[ByteString].map { source =>
        FilePart(partName, filename, contentType, source)
      }
  }

  "handleFilePartAsByteString" must {
    "work" in {
      val testByteString = ByteString("asdf")
      val source = Source.single(testByteString)

      val fileInfo = FileInfo("foo", "foo", None)

      val accumulator = handleFilePartAsByteString(fileInfo)

      accumulator.run(source).flatMap { filePart =>
        filePart.ref.runFold(ByteString())(_ ++ _).map { upload =>
          filePart.key must equal ("foo")
          filePart.filename must equal ("foo")
          upload must equal (testByteString)
        }
      }
    }
  }

  "Multipart.multipartParser" must {
    "work" in {
      fail("comment out this line to see the deadlock")

      lazy val body =
        """
          |--boundary
          |Content-Disposition: form-data; name="file"; filename="foo"
          |Content-Type: text/plain
          |
          |bar
          |--boundary--
          |""".stripMargin.lines.mkString("\r\n")

      val request = FakeRequest().withHeaders(HeaderNames.CONTENT_TYPE -> "multipart/form-data; boundary=boundary")

      val parser = Multipart.multipartParser(Int.MaxValue, handleFilePartAsByteString, httpErrorHandler)
      val accumulator = parser(request)

      accumulator.run(Source.single(ByteString(body))).flatMap { resultOrFormData =>
        val filePart = resultOrFormData.right.value.file("file").get
        filePart.filename must equal("foo")
        filePart.contentType must equal(Some("text/plain"))
        filePart.ref.runFold(ByteString())(_ ++ _).map { byteString =>
          byteString.decodeString("utf-8") must equal("bar")
        }
      }
    }
  }

}
