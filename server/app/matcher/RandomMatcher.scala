package matcher;

import models._
import sorm.Persisted

import org.joda.time.DateTime
import scala.util.Random

object RandomMatcher extends Matcher {

  def apply(users: Iterable[User with Persisted]): Iterable[Match] = {
    val randomUsers = Random.shuffle(users filter (_.profile.isDefined))

    val matches = for { Stream(user1, user2) <- randomUsers.sliding(2, 2) }
      yield Match(user1, user2, false, false, State.Pending, DateTime.now)

    matches.toSeq
  }
}
