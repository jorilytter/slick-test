package fi.jori.slick.db

import scala.slick.driver.H2Driver.simple._
import java.sql.Date

object TaskExample extends App {

  case class User(
      name: String,
      email: String
  )
  
  case class Task(
      id: Int,
      topic: String,
      description: String,
      started: Date,
      finished: Option[Date],
      user: String)
      
  class Users(tag: Tag) extends Table[User](tag, "user") {
    def name = column[String]("name")
    def email = column[String]("email", O.PrimaryKey)
    def * = (name,email) <> (User.tupled, User.unapply _)
  }
  val users = TableQuery[Users]
      
  class Tasks(tag: Tag) extends Table[Task](tag, "task") {
    def id = column[Int]("id", O.PrimaryKey)
    def topic = column[String]("topic")
    def description = column[String]("description")
    def started = column[Date]("started")
    def finished = column[Option[Date]]("finished", O.Nullable)
    def user = column[String]("user")
    def userEmail = foreignKey("user", user, users)(_.email)
    def * = (id,topic,description,started,finished,user) <> (Task.tupled, Task.unapply _)
  }
  val tasks = TableQuery[Tasks]
  
  Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver") withSession {
    implicit session =>
      
      (tasks.ddl ++ users.ddl).create
      
      tasks.ddl.createStatements foreach(println)
      users.ddl.createStatements foreach(println)
      
      users += User("jori","jori@foo.com")
      users += User("jori2","jori@bar.com")
      
      tasks += Task(1,"otsikko","selostus",new Date(System.currentTimeMillis()),None,"jori@bar.com")
      tasks += Task(2,"otsikko","selostus",new Date(System.currentTimeMillis()),None,"jori@foo.com")
      tasks += Task(3,"otsikko","selostus",new Date(System.currentTimeMillis()-5000),Some(new Date(System.currentTimeMillis())),"jori@foo.com")
      
      println("all tasks")
      println(tasks.selectStatement)
      tasks.foreach(println)
      
      def finishTask(id: Int) = tasks filter(t => t.id === id) map (t => t.finished) update (Some(new Date(System.currentTimeMillis())))
      val unfinished = tasks filter (t => t.finished.isNull)
      val finished = tasks filter (t => t.finished.isNotNull)
      val byUser = for {
        u <- users if u.name === "jori2"
        t <- tasks filter (t => t.user === u.email)
      } yield t
      
      println("unfinished tasks")
      println(unfinished.selectStatement)
      unfinished.foreach(println)
      
      println("finished tasks")
      println(finished.selectStatement)
      finished.foreach(println)
      
      println("by user jori2")
      println(byUser.selectStatement)
      byUser.foreach(println)
      
      tasks += Task(4,"otsikko","selostus",new Date(System.currentTimeMillis()-25000),Some(new Date(System.currentTimeMillis())),"jori@foo.com")
      println("two finished tasks")
      println(finished.selectStatement)
      finished.foreach(println)
      
      finishTask(1)
      println(tasks filter(t => t.id === 1) map (t => t.finished) updateStatement)
      println("all but one tasks finished")
      println(finished.selectStatement)
      finished.foreach(println)
      
  }
}