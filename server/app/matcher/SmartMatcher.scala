package matcher

import models._
import org.jgrapht.alg.EdmondsBlossomShrinking
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import org.joda.time.{DateTime, Years}
import sorm.Dsl._
import sorm.Persisted

import scala.collection.JavaConversions
import scala.language.postfixOps

/**
  * Created by Simon and Lucie
  */
object SmartMatcher extends Matcher {

  /**
    * Method matching user 2 by 2 according to their preferences and matching the most users together
    *
    * @param users A collection of User, unsorted
    * @return An iterable of Match
    */
  def apply(users: Iterable[User with Persisted]): Iterable[Match] = {
    // Map user-age
    def ageOfUser(user: User) =
      Years.yearsBetween(user.profile.get.birthday, DateTime.now).getYears

    def checkAgeInterest(u1: User, u2: User): Boolean = {
      val (p1, p2) = (u1.profile.get, u2.profile.get)

      (p1.ageInterest contains ageOfUser(u2)) && (p2.ageInterest contains ageOfUser(u2))
    }

    def checkGenderInterest(u1: User, u2: User): Boolean = {
      val (p1, p2) = (u1.profile.get, u2.profile.get)

      (p1.genderInterest == GenderInterest.Both || (p1.genderInterest == GenderInterest.Male && p2.gender == Gender.Male) || (p1.genderInterest == GenderInterest.Female && p2.gender == Gender.Female)) &&
      (p2.genderInterest == GenderInterest.Both || (p2.genderInterest == GenderInterest.Male && p1.gender == Gender.Male) || (p2.genderInterest == GenderInterest.Female && p1.gender == Gender.Female))
    }

    def checkAlreadyMatched(u1: User, u2: User): Boolean =
      DB.query[Match].where(
        (("user1" equal u1) and ("user2" equal u2)) or
        (("user1" equal u2) and ("user2" equal u1))
      ).exists
    
    def checkCompatibility(u1: User, u2: User): Boolean = {
      checkAgeInterest(u1, u2) && checkGenderInterest(u1, u2) && !checkAlreadyMatched(u1, u2)
    }

    val usersWithProfile = users filter (_.profile.isDefined)

    val graph = new SimpleGraph[User, DefaultEdge](classOf[DefaultEdge])

    usersWithProfile foreach graph.addVertex

    for {
      u1 <- usersWithProfile
      u2 <- usersWithProfile

      if (u1 != u2 && checkCompatibility(u1, u2))
    } graph.addEdge(u1, u2)

    val blossomMatcher = new EdmondsBlossomShrinking[User, DefaultEdge](graph)
    val usersPairs = JavaConversions.asScalaSet(blossomMatcher.getMatching)

    for (pair <- usersPairs)
      yield Match(graph.getEdgeSource(pair), graph.getEdgeTarget(pair), false, false, State.Pending, DateTime.now)
  }

}

