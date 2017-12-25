import models._
import org.joda.time.DateTime
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import matcher._

/**
  * Created by Simon and Lucie
  */
class MatcherStrategySpec extends PlaySpec with OneAppPerSuite {

    def fixture = new {
      DB.query[User].fetch.foreach(DB.delete(_))

      val userA = DB save User("userA@example.com", "User", "A", Set())
      val userB = DB save User("userB@example.com", "User", "B", Set())
      val userC = DB save User("userC@example.com", "User", "C", Set())

      val profileB = DB save Profile(Some(userB), Section.IN, Gender.Female, DateTime.now, Set(), Set(), "", GenderInterest.Male, 20 to 25)
      val avatarB = DB save Avatar(Some(userB), HairColor.Blond, HairStyle.Style2, Eye.Blue, Skin.Medium, Shirt.Style2)

      val matchAB = DB save Match(userA, userB, true, true, State.Open, DateTime.now)
      val matchBC = DB save Match(userB, userC, false, false, State.Close, DateTime.now)
    }

    "SmartMatcher" should {

      "match two compatible people" in {
        DB.query[User].fetch.foreach(DB.delete(_))

        val userA = DB save User("userA@example.com", "User", "A", Set())
        val userB = DB save User("userB@example.com", "User", "B", Set())

        val profileA = DB save Profile(Some(userA), Section.SC, Gender.Male, DateTime.now.withYear(2000), Set(), Set(), "", GenderInterest.Female, 2 to 25)
        val profileB = DB save Profile(Some(userB), Section.IN, Gender.Female, DateTime.now.withYear(2000), Set(), Set(), "", GenderInterest.Male, 2 to 25)

        val matches = SmartMatcher(DB.query[User].fetch)

        println(matches)
      }

      "match multiple compatible people" in {
        DB.query[User].fetch.foreach(DB.delete(_))

        val userA = DB save User("userA@example.com", "User", "A", Set())
        val userB = DB save User("userB@example.com", "User", "B", Set())
        val userC = DB save User("userC@example.com", "User", "C", Set())
        val userD = DB save User("userD@example.com", "User", "D", Set())

        val profileA = DB save Profile(Some(userA), Section.SC, Gender.Male, DateTime.now.withYear(2000), Set(), Set(), "", GenderInterest.Male, 2 to 25)
        val profileB = DB save Profile(Some(userB), Section.IN, Gender.Female, DateTime.now.withYear(2000), Set(), Set(), "", GenderInterest.Both, 2 to 25)
        val profileC = DB save Profile(Some(userC), Section.SC, Gender.Male, DateTime.now.withYear(2000), Set(), Set(), "", GenderInterest.Both, 2 to 25)
        val profileD = DB save Profile(Some(userD), Section.IN, Gender.Female, DateTime.now.withYear(2000), Set(), Set(), "", GenderInterest.Both, 2 to 25)

        val matches = SmartMatcher(DB.query[User].fetch)

        println(matches)
      }

    }
}
