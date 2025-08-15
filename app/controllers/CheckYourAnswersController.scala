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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.AuthenticatedControllerComponents
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckYourAnswersView,
                                            config: FrontendAppConfig
                                          ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>
      
      val waypoints = EmptyWaypoints
      val list = SummaryListViewModel(
        rows = Seq.empty
      )

      Ok(view(waypoints, list, config.iossYourAccountUrl))
  }
  
  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>
      Future.successful(Redirect(CheckYourAnswersPage.navigate(waypoints, request.userAnswers, request.userAnswers).route))
      
  }
}
