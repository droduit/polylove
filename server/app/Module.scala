import com.google.inject.AbstractModule
import notifications.{Notification, Sender}
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[Notification]("notification")
    bindActorFactory[Sender, Sender.Factory]
  }
}
