package matcher

import models.{User, Match}
import sorm.Persisted

trait Matcher {
  def apply(users: Iterable[User with Persisted]): Iterable[Match]
}
