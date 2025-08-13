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
import forms.EuVatNumberFormProvider

import javax.inject.Inject
import pages.{EuCountryPage, EuVatNumberPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.EuVatNumberView

import scala.concurrent.{ExecutionContext, Future}

class EuVatNumberController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: EuVatNumberFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: EuVatNumberView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(EuVatNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      request.userAnswers.get(EuCountryPage).map { country =>
        Ok(view(preparedForm, waypoints, country))
      }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          request.userAnswers.get(EuCountryPage).map { country =>
            Future.successful(BadRequest(view(formWithErrors, waypoints, country)))
          }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(EuVatNumberPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(EuVatNumberPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
