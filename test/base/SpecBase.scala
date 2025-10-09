/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package base

import controllers.actions.*
import date.{Dates, Today, TodayImpl}
import generators.Generators
import models.CountryWithValidationDetails.euCountriesWithVRNValidationRules
import models.{CheckMode, Country, CountryWithValidationDetails, UserAnswers}
import org.scalacheck.Gen
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.*
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Vrn

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId}
import java.util.Locale

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with Generators {

  val userAnswersId: String = "id"
  val vrn: Vrn = Vrn("123456789")
  val intermediaryNumber = "IN9001234567"
  val country: Country = arbitraryCountry.arbitrary.sample.value
  val anotherCountry: Country = Gen.oneOf(Country.euCountries.filterNot(_ == country)).sample.value
  val moveDate: LocalDate = LocalDate.now(Dates.clock)
  val euVatNumber: String = getEuVatNumber(country.code)
  val waypoints: Waypoints = EmptyWaypoints
  val checkModeWaypoints: Waypoints = waypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

  val countryWithValidationDetails: CountryWithValidationDetails =
    euCountriesWithVRNValidationRules.find(_.country == country).value

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "/endpoint").withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

  val arbitraryDate: LocalDate = datesBetween(LocalDate.of(2023, 3, 1), LocalDate.of(2025, 12, 31)).sample.value
  val arbitraryInstant: Instant = arbitraryDate.atStartOfDay(ZoneId.systemDefault).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault)

  def completeUserAnswers: UserAnswers =
    emptyUserAnswers
      .set(MoveCountryPage, true).success.value
      .set(EuCountryPage, country).success.value
      .set(MoveDatePage, moveDate).success.value
      .set(EuVatNumberPage, euVatNumber).success.value

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None,
                                    clock: Option[Clock] = None,
                                  ): GuiceApplicationBuilder = {

    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)

    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[Clock].toInstance(clockToBind),
        bind[Today].toInstance(new TodayImpl(clockToBind))
      )
  }

  def getEuVatNumber(countryCode: String): String =
    CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == countryCode).map { matchedCountryRule =>
      s"$countryCode${matchedCountryRule.exampleVrn}"
    }.value


  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    .withLocale(Locale.UK)
    .withZone(ZoneId.of("GMT"))

}
