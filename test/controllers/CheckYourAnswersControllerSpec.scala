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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import date.{Dates, Today}
import models.{CheckMode, Country}
import org.mockito.Mockito.when
import org.scalatest.PrivateMethodTester
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.checkAnswers.{EuCountrySummary, EuVatNumberSummary, MoveCountrySummary, MoveDateSummary, StoppedUsingServiceDateSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import java.time.LocalDate

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with PrivateMethodTester {

  private val today: LocalDate = LocalDate.now
  private val mockToday: Today = mock[Today]
  when(mockToday.date).thenReturn(today)
  private val date: Dates = new Dates(mockToday)
  private val answers = emptyUserAnswers
    .set(MoveCountryPage, true).success.value
    .set(EuCountryPage, Country("DE", "Germany")).success.value
    .set(MoveDatePage, today).success.value
    .set(EuVatNumberPage, "DE123456789").success.value

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val view = application.injector.instanceOf[CheckYourAnswersView]
        val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
        val list = SummaryListViewModel(
          Seq(
            MoveCountrySummary.row(answers, waypoints, CheckYourAnswersPage),
            EuCountrySummary.row(answers, waypoints, CheckYourAnswersPage),
            MoveDateSummary.row(answers, waypoints, CheckYourAnswersPage, date),
            EuVatNumberSummary.row(answers, waypoints, CheckYourAnswersPage),
            StoppedUsingServiceDateSummary.row(answers, waypoints, CheckYourAnswersPage, date)
          ).flatten
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(waypoints, list, appConfig.iossYourAccountUrl, isValid = true)(request, messages(application)).toString
      }
    }

    "must include StoppedUsingServiceDateSummary row in the summary list when data is present" in {
      val testDate = LocalDate.of(2023, 12, 31)
      val answersWithStoppedUsingServiceDate = answers
        .set(StoppedUsingServiceDatePage, testDate).success.value

      val application = applicationBuilder(userAnswers = Some(answersWithStoppedUsingServiceDate))
        .build()

          running(application) {
            implicit val msgs: Messages = messages(application)
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe controllers.routes.MoveDateController.onPageLoad(waypoints).url
          }
        }

        val result = route(application, request).value
        val view = application.injector.instanceOf[CheckYourAnswersView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
        val dates = application.injector.instanceOf[Dates]
        val list = SummaryListViewModel(
          Seq(
            MoveCountrySummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage),
            EuCountrySummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage),
            MoveDateSummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage, dates),
            EuVatNumberSummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage),
            StoppedUsingServiceDateSummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage, dates)
          ).flatten
        )

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, list, appConfig.iossYourAccountUrl, isValid = true)(request, msgs).toString

        val stoppedUsingServiceDateRow = StoppedUsingServiceDateSummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage, dates)
        stoppedUsingServiceDateRow mustBe defined

        val actualValue = stoppedUsingServiceDateRow.value.value.content.asInstanceOf[Text].value
        val expectedValue = dates.formatter.format(testDate)

        actualValue mustBe expectedValue
      }
    }

    "when the user has not answered all necessary data" - {
      "the user is redirected when the incomplete prompt is shown" - {
        "to the Eu Country page when the EU country is missing" in {
          val answers = completeUserAnswers.remove(EuCountryPage).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe controllers.routes.EuCountryController.onPageLoad(waypoints).url
          }
        }

        "to the Move Date page when the move date is missing" in {
          val answers = completeUserAnswers.remove(MoveDatePage).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe controllers.routes.MoveDateController.onPageLoad(waypoints).url
          }
        }

        "to the EU VAT Number page when the VAT number is missing" in {
          val answers = completeUserAnswers.remove(EuVatNumberPage).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe controllers.routes.EuVatNumberController.onPageLoad(waypoints).url
          }
        }
      }
    }
  }
}