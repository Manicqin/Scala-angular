package main

/**
 * Created by boaz on 2/10/15.
 */

import com.twitter.finatra._
import com.twitter.finatra.ContentType._
import com.twitter.finagle.http.Cookie

import org.squeryl._
import org.squeryl.dsl._
import org.squeryl.SessionFactory
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column

import java.security.MessageDigest

import argonaut._, Argonaut._
import scalaz._, Scalaz._
import com.typesafe.config._

import adapters.{MSSQLServer, PostgreSqlAdapter, OracleAdapter, MySQLAdapter, DerbyAdapter}

object Test{


  abstract class TableBase
  case class User(@Column("fisrt")
              val first:String,
             val last: String,
             val office: String,
                   val username : String,
                   val password: String) extends TableBase{
    val id : Int = 0

    def this() = this("","","","","")

  }

  case class Asset(val name:String,
              val publicdescription: String,
              val privatedescription: String,
              val requireautentication: Boolean) extends TableBase{
    val id : Int = 0
    def this() = this("","","",false)

  }

  case class Owner(val userid : Int,
                   val assetid : Int) extends TableBase{


    def this() = this(0,0)

  }

  implicit def UserCodecJson =
    casecodec5(User.apply, User.unapply)("first", "last", "office","username","password")

//  implicit def UserEncodeJson: EncodeJson[User] =
//    EncodeJson((p: User) =>
//        ("name" := p.first) ->:
//        ("second" := p.last) ->:
//        ("office" := p.office) ->: jEmptyObject)


  implicit def AssetCodecJson =
    casecodec4(Asset.apply, Asset.unapply)("name", "publicdescription", "privatedescription","requireautentication")

  implicit def OwnerCodecJson =
    casecodec2(Owner.apply, Owner.unapply)("userid", "assetid")

  object security {
    private def salt = "1qazXSW@"
    def md5(s: String) = {
      MessageDigest.getInstance("MD5").digest((s+salt).getBytes())
    }
  }
  object Library extends Schema {

    val Users = table[User]("users")
    val Assets = table[Asset]("assests")
    val Owners = table[Owner]("owners")

    val vUsers = view[User]("users")
    val vAssets = view[Asset]("assests")
    val vOwners = view[Owner]("owners")

    val getViews = Map("users"->vUsers,
                      "assets"->vAssets,
                      "owners"->vOwners)

    val getDBs = Map("users"->Users,
                      "assets"->Assets,
                      "owners"->Owners)
  }

  val sql = Library

  Class.forName("org.postgresql.Driver")

  def printDataBase[T](db:Table[T]):Query[T] = from(db)(select(_))
  def printView[T](db:org.squeryl.View[T]):Query[T] = from(db)(select(_))

  def connect(): Session = {
    val conn = java.sql.DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres","boaz","1qazXSW@3")
    val retVal = Session.create(conn, new PostgreSqlAdapter)
    println("connected")
    return retVal
  }
  SessionFactory.concreteFactory = Some(()=> connect())

}


object App extends FinatraServer {

  //val config = ConfigFactory.load()
  //val version = config.getString("api.version")
  ////println(version)
  class ExampleApp extends Controller {

    get("/") { request =>
      val cookie = request.cookies("loggedIn")
      if(cookie.value == "true")
        render.static("index.html").toFuture
      else
        render.static("signin.html").toFuture
    }
    get("/logout") { request =>
      render.plain("out")
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
            val test = Test.printView(Test.Library.getViews("users"))

            inTransaction {

              //from(Test.Library.vUsers)(s => where(s.username === user) select(s))
              val userPass = from(Test.Library.Users)(s => where(s.username === user) select (s))
              if (userPass nonEmpty) {
                if (userPass.single.password == password) {
                  retVal = Json("userName" -> jString(userPass.single.first),"logged" -> jBool(true))
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
        .cookie("loggedIn", logged)
        .toFuture
    }

    get("/printDB") { request =>

      val db = request.params.get("db")
      var jsonArr = Json.jEmptyArray
      var strArr : List[String] = Nil
      val test = Test.printView(Test.Library.getViews(db get))

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

  register(new ExampleApp())

}