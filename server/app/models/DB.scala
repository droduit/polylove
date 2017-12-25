package models

import sorm._

/**
  * Created by thierry on 01.10.16.
  */
object DB extends Instance (
  entities = Seq(Entity[Message](), Entity[User](), Entity[Profile](), Entity[Avatar](), Entity[Match]()),
  url = "jdbc:h2:mem:test"
)
