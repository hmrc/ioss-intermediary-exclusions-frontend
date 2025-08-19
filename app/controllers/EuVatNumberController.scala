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
import models.{CountryWithValidationDetails, UserAnswers}

import javax.inject.Inject
import pages.{EuCountryPage, EuVatNumberPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.EuVatNumberView
import utils.FutureSyntax.FutureOps

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

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      getCountryWithValidationDetails(request.userAnswers).map { countryWithValidationDetails =>
        val form = formProvider(countryWithValidationDetails.country)
        val preparedForm = request.userAnswers.get(EuVatNumberPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, countryWithValidationDetails))
      }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      getCountryWithValidationDetails(request.userAnswers).map { countryWithValidationDetails =>
        formProvider(countryWithValidationDetails.country).bindFromRequest().fold(
          formWithErrors => BadRequest(view(formWithErrors, waypoints, countryWithValidationDetails)).toFuture,
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(EuVatNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(EuVatNumberPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))

  }

  private def getCountryWithValidationDetails(userAnswers: UserAnswers): Option[CountryWithValidationDetails] = {
    userAnswers.get(EuCountryPage).flatMap(country =>
      CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == country.code)
    )
  }
}
