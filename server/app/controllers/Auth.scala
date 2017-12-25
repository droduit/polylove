package controllers

import models.{DB, User}
import sorm.Persisted
import play.api.mvc.RequestHeader
import play.api.mvc.Security.AuthenticatedBuilder

trait Auth {
	object Authenticated extends AuthenticatedBuilder(req => authenticate(req))

	def authenticate(req: RequestHeader): Option[User with Persisted] =
		try {
			req.session.get("userId").map(_.toInt).map(DB.fetchById[User](_))
		} catch {
			case e : Exception => None
		}
}
