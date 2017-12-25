package services

import javax.inject._

import akka.actor._
import com.google.inject.AbstractModule
import models._
import notifications.Notification
import org.joda.time._
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import sorm.Persisted

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

import matcher.SmartMatcher

class MatcherModule extends AbstractModule {
  def configure() = {
    bind(classOf[Matcher]).asEagerSingleton()
  }
}

@Singleton
class Matcher @Inject()(val system: ActorSystem, @Named("notification") notification: ActorRef, applicationLifecycle: ApplicationLifecycle) {

  private val startMatchTask = new Runnable() {
    override def run = {
      Logger.debug("Time for a match!")

      val matches = SmartMatcher(DB.query[User].fetch)

      matches map (DB save _) foreach { _match =>
        val u1 = _match.user1.asInstanceOf[User with Persisted]
        val u2 = _match.user2.asInstanceOf[User with Persisted]

        val notifdata = Json.obj("subject" -> "new-match", "match" -> _match.id, "user1" -> u1.id, "user2" -> u2.id)

        notification ! Notification.SendTo(u1, None, Some(notifdata))
        notification ! Notification.SendTo(u2, None, Some(notifdata))
      }
    }
  }

  private val endMatchTask = new Runnable() {
    override def run = {
      Logger.debug("Time to send the status")

      val matches = DB.query[Match].whereEqual("state", State.Pending).fetch()

      for (m <- matches){
        val inLove = m.user1Interested && m.user2Interested
        val newState = if (inLove) State.Open else State.Close
        val updatedMatch = DB save (m copy (state = newState))
        val notifdata = Json.obj("subject" -> "end-match", "matchId" -> m.id, "state" -> newState)
        notification ! Notification.SendTo(m.user1, None, Some(notifdata))
        notification ! Notification.SendTo(m.user2, None, Some(notifdata))
      }
    }
  }

  private def millisUntil(hour: Int) = {
    val now = DateTime.now()
    if (now.getHourOfDay() < hour)
      new org.joda.time.Duration(now, now.withHourOfDay(hour)).getMillis
    else
      new org.joda.time.Duration(now, now.plusDays(1).withHourOfDay(hour)).getMillis
  }

  private val startMatchScheduler = system.scheduler.schedule(
    millisUntil(9) millis, 1 days, startMatchTask)

  private val endMatchScheduler = system.scheduler.schedule(
    millisUntil(21) millis, 1 days, endMatchTask)

  applicationLifecycle addStopHook { () =>
    Future {
      startMatchScheduler.cancel
      endMatchScheduler.cancel
    }
  }
}
