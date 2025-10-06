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
import date.Dates
import forms.StoppedUsingServiceDateFormProvider
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, StoppedUsingServiceDatePage, Waypoints}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.StoppedUsingServiceDateView

import java.time.LocalDate
import scala.concurrent.Future

class StoppedUsingServiceDateControllerSpec extends SpecBase with MockitoSugar {

  implicit val messages: Messages = stubMessages()
  val formProvider = new StoppedUsingServiceDateFormProvider()
  private def form(currentDate: LocalDate = LocalDate.now(), registrationDate: LocalDate = LocalDate.now()): Form[LocalDate] =
    formProvider.apply(currentDate, registrationDate)
  val emptyWaypoints: Waypoints = EmptyWaypoints
  val validAnswer: LocalDate = LocalDate.now(stubClockAtArbitraryDate)

  override val emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  lazy val stoppedUsingServiceDateRoute: String = routes.StoppedUsingServiceDateController.onPageLoad(emptyWaypoints).url

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, stoppedUsingServiceDateRoute)
      .withFormUrlEncodedBody(
        "value.day" -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year" -> validAnswer.getYear.toString
      )

  "StoppedUsingServiceDate Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, stoppedUsingServiceDateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StoppedUsingServiceDateView]
        val dates = application.injector.instanceOf[Dates]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form(), emptyWaypoints, dates.dateHint)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(StoppedUsingServiceDatePage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, stoppedUsingServiceDateRoute)

        val view = application.injector.instanceOf[StoppedUsingServiceDateView]
        val dates = application.injector.instanceOf[Dates]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form().fill(validAnswer), emptyWaypoints, dates.dateHint)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value
        val userAnswers = emptyUserAnswers
          .set(StoppedUsingServiceDatePage, validAnswer).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual StoppedUsingServiceDatePage.navigate(emptyWaypoints, emptyUserAnswers, userAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, stoppedUsingServiceDateRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form().bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[StoppedUsingServiceDateView]
        val dates = application.injector.instanceOf[Dates]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, emptyWaypoints, dates.dateHint)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, stoppedUsingServiceDateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, stoppedUsingServiceDateRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
