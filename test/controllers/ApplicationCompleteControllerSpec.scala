package controllers

import base.SpecBase
import config.FrontendAppConfig
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ApplicationCompleteView

class ApplicationCompleteControllerSpec extends SpecBase {

  "ApplicationComplete Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(config.iossYourAccountUrl)(request, messages(application)).toString
      }
    }
  }
}
