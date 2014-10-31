package scaloid.example

import java.io.IOException

import android.app.Activity
import android.content.Context
import android.os.{ Bundle, AsyncTask }
import android.text.Layout
import android.view.ViewGroup.{ MarginLayoutParams, LayoutParams }
import android.view.{ ViewGroup, View }
import android.graphics.Color
import android.widget.{ TextView, Button, LinearLayout, FrameLayout }
import org.scaloid.common.{ play => pl, toast => sToast, _ }

import play.api.libs.json._
import play.api.libs.json.Json._

import com.squareup.okhttp._

import scala.concurrent.{ ExecutionContext, Promise, Future }
import scala.util.{ Success, Failure }
import scala.language.implicitConversions

import macroid._
import macroid.Contexts
import macroid.FullDsl._
import macroid.contrib.TextTweaks._
import macroid.contrib.LpTweaks._
import macroid.Ui

object Layouts {
  import JsonClasses._

  def margin(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0, all: Int = 0): Tweak[View] = if (all >= 0) {
    Tweak[View](_.setLayoutParams(marginParams(all, all, all, all)))
  } else {
    Tweak[View](_.setLayoutParams(marginParams(left, top, right, bottom)))
  }

  def marginParams(left: Int, top: Int, right: Int, bottom: Int): MarginLayoutParams = {
    val marginParams = new MarginLayoutParams(0, 0)
    marginParams.setMargins(left, top, right, bottom)
    marginParams
  }

  /**
   * Must be called between "super.onCreate(savedInstanceState) and setContentView(getUi(view))"
   *
   * Example: {{{
   * class HelloScaloid extends Activity with Contexts[Activity] {
   *   override def onCreate(savedInstanceState: Bundle) = {
   *     super.onCreate(savedInstanceState)
   *
   *     val layout = Layouts.layoutOne
   *     setContentView(getUi(layout))
   *   }
   * }
   * }}}
   */
  def layoutOne(
    implicit
    ctx:        ActivityContext,
    appCtx:     AppContext,
    exCtx:      ExecutionContext,
    httpClient: OkHttpClient
  ): Ui[FrameLayout] = {
    val wat = 5.dp
    l[FrameLayout](
      l[LinearLayout](
        w[Button] <~ text("Simple JSON") <~ margin(all = wat) <~ matchWidth <~ On.click(toast(parseJson) <~ fry),
        w[Button] <~ text("Optional JSONs") <~ margin(all = wat) <~ matchWidth <~ On.click(toast(parseOptionalJson) <~ fry),
        w[Button] <~ text("Vibrate") <~ margin(all = wat) <~ matchWidth <~ On.click(Ui(vibrator(appCtx.app).vibrate(1000))),
        w[Button] <~ text("HTTP Get") <~ margin(all = wat) <~ matchWidth <~ On.click(Ui(httpGet.map(x => runUi(toast(x) <~ fry))))
      ) <~ vertical <~ matchParent
    )
  }

  def parseJson: String = {
    implicit val personFormat = Json.format[Demo]
    val json = """
     |[{"age": 1, "gender": "female"}]
    """.stripMargin

    //    val wut = json.decodeOption[List[Demo]]
    val wut = Json.fromJson[List[Demo]](parse(json)).asOpt
    wut match {
      case Some(demo) => Json.toJson(demo).toString()
      case None       => "Error parsing JSON"
    }
  }

  def parseOptionalJson: String = {
    val json = """
     |[{"wheels": 4, "doors": 4}, {"wheels": 2, "doors": null}, {"wheels": 2}]
    """.stripMargin
    implicit val vehicleFormat = Json.format[Vehicle]

    Json.fromJson[List[Vehicle]](parse(json)).asOpt match {
      case Some(vehicles) => Json.toJson(vehicles).toString()
      case None           => "Error parsing JSON"
    }
  }

  def httpGet(implicit ctx: ExecutionContext, client: OkHttpClient): Future[String] = {
    val request = HttpRequest().url("http://publicobject.com/helloworld.txt")
    val response = HttpRequest.execute(request, 200)
    response map { res =>
      res.body.string()
    } recover {
      case _ =>
        "Something went wrong..."
    }
  }
}

object JsonClasses {
  case class Demo(age: Int, gender: String)
  case class Vehicle(wheels: Int, doors: Option[Int])
}

class HelloScaloid extends Activity with Contexts[Activity] {
  implicit val exec = ExecutionContext.fromExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
  implicit val client = new OkHttpClient

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)

    val layout = Layouts.layoutOne
    setContentView(getUi(layout))
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