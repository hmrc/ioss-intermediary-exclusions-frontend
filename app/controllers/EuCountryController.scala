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

import controllers.actions.*
import forms.EuCountryFormProvider
import models.{Country, UserAnswers}
import pages.{EuCountryPage, EuVatNumberPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.EuCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuCountryController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     cc: AuthenticatedControllerComponents,
                                     formProvider: EuCountryFormProvider,
                                     view: EuCountryView
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Country] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>

      val preparedForm = request.userAnswers.get(EuCountryPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>
          for {
            updatedAnswersEuVatNumber <- updateEuVatNumberPage(request.userAnswers, value)
            updatedAnswers <- Future.fromTry(updatedAnswersEuVatNumber.set(EuCountryPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(EuCountryPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }

  private def updateEuVatNumberPage(userAnswers: UserAnswers, country: Country): Future[UserAnswers] =
    if (userAnswers.get(EuCountryPage).contains(country)) {
      userAnswers.toFuture
    } else {
      Future.fromTry(userAnswers.remove(EuVatNumberPage))
    }
}
