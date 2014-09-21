package scaloid.example

import java.io.IOException
import java.util.logging.Level

import android.net.Uri
import android.os.AsyncTask
import com.squareup.okhttp._
import org.scaloid.common._
import android.graphics.Color

import scala.concurrent.{ ExecutionContext, Promise, Future }
import scala.util.{ Success, Failure }

class HelloScaloid extends SActivity {
  implicit val exec = ExecutionContext.fromExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
  implicit val client = new OkHttpClient

  onCreate {
    val wtf = new SFrameLayout {
      this += new SVerticalLayout {
        STextView("Enter your password").<<.wrap.>>
        SEditText().inputType(TEXT_PASSWORD)
        STextView("Repeat your password").<<.wrap.>>
        SEditText().inputType(TEXT_PASSWORD)
        SButton("HHHH")
        SButton("first").textSize(20.dip).<<.margin(5.dip).>>.onClick {
          vibrator.vibrate(1000)
        }
        SButton("HTTP call success").textSize(20.dip).<<.margin(5.dip).>>.onClick {
          val request = HttpRequest().url("http://publicobject.com/helloworld.txt")
          val response = HttpRequest.execute(request, 200)
          response.onComplete {
            case Success(res) =>
              toast(res.body.string())
            case Failure(e) =>
              runOnUiThread { toast("Something went wrong...") }
          }
        }
      }.<<.fill.>>
    }

    contentView = wtf
  }
}

object HttpRequest {
  implicit val tag = LoggerTag("HelloScaloid")

  import scala.language.implicitConversions

  def apply(): Request.Builder = {
    val wtf = new Request.Builder()
    wtf
  }

  implicit def mapToHeader(headers: Map[String, String]): Headers = {
    val _headers = new Headers.Builder
    headers.foreach {
      case (k, v) =>
        _headers.add(k, v)
    }
    _headers.build()
  }

  implicit def mapToHeader(headers: List[(String, String)]): Headers = {
    val _headers = new Headers.Builder
    headers.foreach {
      case (k, v) =>
        _headers.add(k, v)
    }
    _headers.build()
  }

  def queryString(queryParams: Map[String, Seq[String]]): String = {
    queryParams.map {
      case (paramName, paramValues) =>
        paramValues.map { value => s"$paramName=$value" }.mkString("&")
    }.mkString("?", "&", "")
  }

  def execute(request: Request.Builder)(implicit client: OkHttpClient): Future[Response] = {
    val call = client.newCall(request.build())

    val output = Promise[Response]()
    call.enqueue(new Callback() {
      def onFailure(request: Request, e: IOException): Unit = {
        warn("Failed to execute "+request, e)
        output.complete(Failure(e))
      }

      def onResponse(response: Response): Unit = {
        output.complete {
          Success(response)
        }
      }
    })
    output.future
  }

  def execute(request: Request.Builder, expectedCode: Int)(implicit client: OkHttpClient): Future[Response] = {
    val call = client.newCall(request.build())

    val output = Promise[Response]()
    call.enqueue(new Callback() {
      def onFailure(request: Request, e: IOException): Unit = {
        warn("Failed to execute "+request, e)
        output.complete(Failure(e))
      }

      def onResponse(response: Response): Unit = {
        output.complete {
          response.code() match {
            case actualCode if actualCode == expectedCode => Success(response)
            case actualCode                               => Failure(UnexpectedResponseStatus(expectedCode, actualCode))
          }
        }
      }
    })
    output.future
  }

  case class UnexpectedResponseStatus(expectedCode: Int, actualCode: Int) extends IOException(
    s"HTTP Request failed. Expected code: $expectedCode. Actual code: $actualCode"
  )
}