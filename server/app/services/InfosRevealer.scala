package services

import javax.inject._

import akka.actor._
import com.google.inject.AbstractModule
import models._
import notifications.Notification.SendTo
import org.joda.time.DateTime
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import sorm.Persisted

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class InfosRevealerModule extends AbstractModule {
  def configure() = {
    bind(classOf[InfosRevealer]).asEagerSingleton()
  }
}

@Singleton
class InfosRevealer @Inject()(val system: ActorSystem, @Named("notification") notification: ActorRef, applicationLifecycle: ApplicationLifecycle) {

  // General reveal set for all matches
  private val alreadyRevealed = scala.collection.mutable.Map.empty[Long, Set[String]]

  val revealTask = new Runnable() {
    override def run() = {
      Logger.debug("Revealing infos to the users")

      val matches = DB.query[Match].whereEqual("state", State.Pending).fetch

      matches foreach { _match =>

        val u1 = _match.user1.asInstanceOf[User with Persisted]
        val p1 = u1.profile.get

        val u2 = _match.user2.asInstanceOf[User with Persisted]
        val p2 = u2.profile.get

        // Add infos to a set
        var infosSet = Set[String]()

        if (p1.section != p2.section) {
          infosSet += s"Someone's section is ${p1.section}"
          infosSet += s"Someone's section is ${p2.section}"
        } else {
          infosSet += s"You both's section is ${p1.section}"
        }

        infosSet += s"Someone's birthday is the ${p1.birthday.toString("dd MM yyyy")}"
        infosSet += s"Someone's birthday is the ${p2.birthday.toString("dd MM yyyy")}"

        infosSet += s"Here's someone's description: ${p1.description}"
        infosSet += s"Here's someone's description: ${p2.description}"

        infosSet ++= p1.hobbies map (h => s"Here's someone's hobby: $h")
        infosSet ++= p2.hobbies map (h => s"Here's someone's hobby: $h")

        // Add info to reveal to revealed infos list
        val revealed = alreadyRevealed.getOrElse(_match.id, Set())
        val infosToReveal = (infosSet -- revealed).toList(Random.nextInt(infosSet.size))
        alreadyRevealed(_match.id) = revealed + infosToReveal

        // Store message with ID 0 for server
        val storedMessage = DB save Message(_match, None, infosToReveal, DateTime.now())
        val notif = Json.obj(
          "subject" -> "new-message",
          "matchId" -> _match.id, "senderId" -> 0,
          "messageId" -> storedMessage.id, "content" -> storedMessage.content, "sentAt" -> storedMessage.sentAt
        )

        notification ! SendTo(_match.user2, None, Some(notif))
        notification ! SendTo(_match.user1, None, Some(notif))
      }
    }
  }

  private def millisUntil(hour: Int) = {
    val now = DateTime.now()
    if (now.getHourOfDay < hour)
      new org.joda.time.Duration(now, now.withHourOfDay(hour)).getMillis
    else
      new org.joda.time.Duration(now, now.plusDays(1).withHourOfDay(hour)).getMillis
  }

  private val reveal12Scheduler = system.scheduler.schedule(
    millisUntil(12) millis, 1 days, revealTask)

  private val reveal15Scheduler = system.scheduler.schedule(
    millisUntil(15) millis, 1 days, revealTask)

  private val reveal18Scheduler = system.scheduler.schedule(
    millisUntil(18) millis, 1 days, revealTask)

  private val reveal20Scheduler = system.scheduler.schedule(
    millisUntil(20) millis, 1 days, revealTask)

  applicationLifecycle addStopHook { () =>
    Future {
      reveal12Scheduler.cancel
      reveal15Scheduler.cancel
      reveal18Scheduler.cancel
      reveal20Scheduler.cancel
    }
  }
}
