package main

/**
 * Created by boaz on 2/10/15.
 */

import com.twitter.finatra._

import org.squeryl._
import org.squeryl.SessionFactory
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Table

import java.security.MessageDigest

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import adapters.{PostgreSqlAdapter}

import java.sql.Timestamp

object DbHandler{

  abstract class TableBase

  case class User(val first:String,
                    val last: String,
                    val office: String,
                    val email : String) extends TableBase{
    val id : Int = 0

    def this() = this("","","","")
  }

  case class Asset(val name: String,
                    val descriptionpub: String,
                    val descriptionpri: String,
                    val message: String,
                    val authorizationneeded: Boolean,
                    val notificationneeded: Boolean,
                    val location: String) extends TableBase{
    val id : Int = 0
    def this() = this("","","","",false,false,"")
  }

  case class Owner(val id_user : Int,
                    val id_asset : Int) extends TableBase{


    def this() = this(0,0)
  }

  case class Login(val id_user : Int,
                    val username : String,
                    val password: String) extends TableBase{


    def this() = this(0,"","")
  }

	case class Event(val id_user : Int,
                     val id_asset : Int,
                     val eventStart : Timestamp,
                     val eventEnd : Timestamp) extends TableBase{


    def this() = this(0,0,new Timestamp(0L),new Timestamp(0L))
  }

  implicit def TimestampEncodeJson: EncodeJson[Timestamp] =
    EncodeJson((p: Timestamp) =>
      (StringToStringWrap("time") := p.getTime) ->: jEmptyObject)

  implicit def TimestampDecodeJson: DecodeJson[Timestamp] =
    DecodeJson(c => for {
      time <- (c --\ "time").as[Long]
    } yield new Timestamp(time))

  implicit def UserCodecJson =
    casecodec4(User.apply, User.unapply)("first", "last", "office","email")

  implicit def AssetCodecJson =
    casecodec7(Asset.apply, Asset.unapply)("name",
      "descriptionpub",
      "descriptionpri",
      "message",
      "authorizationneeded",
      "notificationneeded",
      "location")

  implicit def OwnerCodecJson =
    casecodec2(Owner.apply, Owner.unapply)("id_user", "id_asset")

  implicit def LoginCodecJson =
    casecodec3(Login.apply, Login.unapply)("id_user", "username", "password")

  implicit def EventCodecJson =
    casecodec4(Event.apply, Event.unapply)("id_user", "id_asset", "eventStart", "eventEnd")

  object security {
    private def salt = "1qazXSW@"
    def md5(s: String) = {
      MessageDigest.getInstance("MD5").digest((s+salt).getBytes())
    }
  }

  object Library extends Schema {
    val Users = table[User]("users")
    val Assets = table[Asset]("assets")
    val Owners = table[Owner]("owners")
    val Logins = table[Login]("logins")
    val Events = table[Event]("events")

    val vUsers = view[User]("users")
    val vAssets = view[Asset]("assets")
    val vOwners = view[Owner]("owners")
    val vLogins = view[Login]("logins")
    val vEvents = view[Event]("events")

    val getViews = Map("users"->vUsers,
      "assets"->vAssets,
      "owners"->vOwners,
      "logins"->vLogins,
      "events"->vEvents)

    val getDBs = Map("users"->Users,
      "assets"->Assets,
      "owners"->Owners,
      "logins"->Logins,
      "events"->Events)


  }

  val sql = Library
  Class.forName("org.postgresql.Driver")

  def printDataBase[T](db:Table[T]):Query[T] = from(db)(select(_))
  def printView[T](db:org.squeryl.View[T]):Query[T] = from(db)(select(_))

  def connect(): Session = {
    val conn = java.sql.DriverManager.getConnection("jdbc:postgresql://localhost:5432/web_backend","boaz","1qazXSW@3")
    val retVal = Session.create(conn, new PostgreSqlAdapter)
    return retVal
  }

  if (SessionFactory.concreteFactory == None)
    SessionFactory.concreteFactory = Some(()=> connect())


}


object WebBackEndApp extends FinatraServer {

  class WebApp extends Controller {

    get("/") { request =>
      val gotoLogin = render.static("signin.html").toFuture
      val gotoWork = render.static("index.html").toFuture

      val cookie = request.cookies.get("user")
      var retVal = gotoLogin
      if(cookie isEmpty) {
        gotoLogin
      }
      else{

        val content = Parse.parse(cookie.get.value)

        retVal = content match {
          case \/-(x) => {
            println("right")
            val logged = x.fieldOrEmptyString("logged").toString
            val user = x.fieldOrEmptyString("userName").toString

            if (logged == "true")
              gotoWork
            else
              gotoLogin
          }
          case -\/(x) => gotoLogin
        }
      }

      retVal
    }

    get("/logout") { request =>
      render.plain("Good Bye")
        .cookie("loggedIn", "false")
        .toFuture
    }

    post("/login") { request =>

      var retVal = Json("logged" -> jString("false"))
      var logged = "false"
      val content = Parse.parse(request.contentString)

      val obj = content match {
        case \/-(x) => Map("user" -> x.fieldOrEmptyString("user").toString,"password" -> x.fieldOrEmptyString("password").toString)
        case -\/(x) => x
      }

      obj match {
        case x:String => println(x)
        case x:Map[_,_]=> {
          val res = obj.asInstanceOf[Map[String,String]]

          try {

            val user = res("user").replace("\"","")
            val password = res("password").replace("\"","")
            val logins = DbHandler.printView(DbHandler.Library.getViews("logins"))

            inTransaction {

              //from(Test.Library.vUsers)(s => where(s.username === user) select(s))
              val userLogin = from(DbHandler.sql.Logins)(s => where(s.username === user) select (s))
              if (userLogin nonEmpty) {
                if (userLogin.single.password == password) {
                  val userAccount = from(DbHandler.sql.Users)(s => where(s.id === userLogin.single.id_user) select (s))

                  retVal = Json("userName" -> jString(userAccount.single.last + " " + userAccount.single.first),"logged" -> jBool(true))
                  logged = "true"
                }
              }
            }
          }catch {
            case e: Exception => println(e.getMessage)
          }
        }
      }

      println(retVal.spaces4)
      render.json(retVal.nospaces)
        .cookie("user", retVal.nospaces)
        .toFuture
    }

    def getCCParams(cc: AnyRef) =
      (Map[String, Any]() /: cc.getClass.getDeclaredFields) {(a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(cc))
      }

    get("/queryDB") { request =>

      val db = request.params.get("db")
      var jsonArr = Json.jEmptyArray
      var strArr: List[String] = Nil

      db.get match {
        case "users" =>  println("users")

      }

      println(jsonArr.spaces4)
      render.plain(jsonArr.nospaces).toFuture
    }

    get("/testPage") { request =>

      val db = request.params.get("page")
      println(db)
      render.static(db.get)
        .toFuture
    }

    get("/printDB") { request =>

      val db = request.params.get("db")
      var jsonArr = Json.jEmptyArray
      var strArr : List[String] = Nil
      val test = DbHandler.printView(DbHandler.Library.getViews(db get))

      inTransaction{

        for(item <- test) {

          strArr = item.toString :: strArr

          item match {
            case x:Test.User =>
              jsonArr = item.asInstanceOf[Test.User].asJson -->>: jsonArr

            case x:Test.Asset =>
              jsonArr = item.asInstanceOf[Test.Asset].asJson -->>: jsonArr

            case x:Test.Owner =>
              jsonArr = item.asInstanceOf[Test.Owner].asJson -->>: jsonArr

            case _ => jsonArr = jEmptyArray
          }

        }

      }
      println(jsonArr.spaces4)
      render.plain(jsonArr.nospaces).toFuture
    }

    notFound { request =>
      route.get("/")
    }



  }

  register(new WebApp())

}