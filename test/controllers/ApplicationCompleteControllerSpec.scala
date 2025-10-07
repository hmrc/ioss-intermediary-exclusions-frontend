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
import date.Dates
import pages.{EuCountryPage, MoveCountryPage, MoveDatePage, StoppedUsingServiceDatePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ApplicationCompleteView

import java.time.{Clock, LocalDate, ZoneId}

class ApplicationCompleteControllerSpec extends SpecBase {

  val today: LocalDate = LocalDate.of(2024, 1, 25)
  val clock: Clock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault())

  "ApplicationComplete Controller" - {

    "when someone moves business" - {

      "must return OK and the correct view for a GET" in {

        val moveDate = today.plusDays(1)

        val userAnswers = emptyUserAnswers
          .set(MoveCountryPage, true).success.get
          .set(EuCountryPage, country).success.get
          .set(MoveDatePage, moveDate).success.get

        val application = applicationBuilder(userAnswers = Some(userAnswers), clock = Some(clock))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ApplicationCompleteView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val dates = application.injector.instanceOf[Dates]

          status(result) mustEqual OK
          val leaveDate = moveDate.format(dates.formatter)
          val maxMoveDate = moveDate.plusMonths(1).withDayOfMonth(dates.MoveDayOfMonthSplit).format(dates.formatter)
          contentAsString(result) mustEqual view(
            config.iossYourAccountUrl,
            leaveDate,
            maxMoveDate,
            Some(messages(application)("applicationComplete.moving.text", country.name, maxMoveDate)),
            Some(messages(application)("applicationComplete.next.info.bottom.p1", country.name, maxMoveDate))
          )(request, messages(application)).toString
        }
      }

      "must return OK with the leave date being the 10th of next month (10th Feb)" in {

        val moveDate = today.plusDays(1)

        val userAnswers = emptyUserAnswers
          .set(MoveCountryPage, true).success.get
          .set(EuCountryPage, country).success.get
          .set(MoveDatePage, moveDate).success.get

        val application = applicationBuilder(userAnswers = Some(userAnswers), clock = Some(clock))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ApplicationCompleteView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK
          val leaveDate = "25 January 2024"
          val maxMoveDate = "10 February 2024"
          contentAsString(result) mustEqual view(
            config.iossYourAccountUrl,
            leaveDate,
            maxMoveDate,
            Some(messages(application)("applicationComplete.moving.text", country.name, maxMoveDate)),
            Some(messages(application)("applicationComplete.next.info.bottom.p1", country.name, maxMoveDate))
          )(request, messages(application)).toString
        }
      }
    }

    "when someone stops using the service" - {

      "must return OK and the correct view for a GET" - {
        "when stopping at least 15 days prior to the end of the month (16th Jan)" in {

          val stoppedUsingServiceDate = LocalDate.of(2024, 1, 16)

          val userAnswers = emptyUserAnswers
            .set(MoveCountryPage, false).success.get
            .set(StoppedUsingServiceDatePage, stoppedUsingServiceDate).success.get

          val application = applicationBuilder(userAnswers = Some(userAnswers), clock = Some(clock))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[ApplicationCompleteView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            status(result) mustEqual OK
            val leaveDate = "1 February 2024"
            val maxChangeDate = "1 February 2024"
            contentAsString(result) mustEqual view(config.iossYourAccountUrl, leaveDate, maxChangeDate)(request, messages(application)).toString
          }
        }

        "when stopping less than 15 days prior to the end of the month (17th Jan)" in {

          val stoppedUsingServiceDate = LocalDate.of(2024, 1, 17)

          val userAnswers = emptyUserAnswers
            .set(MoveCountryPage, false).success.get
            .set(StoppedUsingServiceDatePage, stoppedUsingServiceDate).success.get

          val application = applicationBuilder(userAnswers = Some(userAnswers), clock = Some(clock))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[ApplicationCompleteView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            status(result) mustEqual OK
            val leaveDate = "1 February 2024"
            val maxChangeDate = "1 March 2024"
            contentAsString(result) mustEqual view(config.iossYourAccountUrl, leaveDate, maxChangeDate)(request, messages(application)).toString
          }
        }
      }
    }

    "must redirect to JourneyRecoveryController when data is missing" in {

      val userAnswers = emptyUserAnswers

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
