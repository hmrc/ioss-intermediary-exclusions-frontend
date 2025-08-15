/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import models.requests.DataRequest
import pages.*
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}


trait CompletionChecks {

  private def isEuCountryValid()(implicit request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(EuCountryPage).isDefined

  private def isMoveDateValid()(implicit request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(MoveDatePage).isDefined

  private def isEuVatNumberValid()(implicit request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(EuVatNumberPage).isDefined

  private def hasStoppedUsingServiceDate()(implicit request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(StoppedUsingServiceDatePage).isDefined

  private def checkHasMovedCountryJourney()(implicit request: DataRequest[AnyContent]): Boolean = {
    val hasMovedCountry = request.userAnswers.get(MoveCountryPage).contains(true)
    if (hasMovedCountry) {
      isEuCountryValid() && isMoveDateValid() && isEuVatNumberValid()
    } else {
      false
    }
  }

  private def checkStoppedUsingServiceJourney()(implicit request: DataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(LeaveSchemePage).exists {
      case true =>
        hasStoppedUsingServiceDate()
      case false =>
        false
    }
  }

  def validate()(implicit request: DataRequest[AnyContent]): Boolean = {
    checkHasMovedCountryJourney() ||
      checkStoppedUsingServiceJourney()
  }

  def getFirstValidationErrorRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    checkMoveCountryRedirects(waypoints) orElse
      incompleteStoppedUsingServiceRedirect(waypoints) orElse
      noJourneyAnswered()

  }

  private def checkMoveCountryRedirects(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    if (request.userAnswers.get(MoveCountryPage).contains(true)) {
      incompleteEuCountryRedirect(waypoints) orElse
        incompleteMoveDateRedirect(waypoints) orElse
        incompleteEuVatNumberRedirect(waypoints)
    } else {
      None
    }
  }

  private def incompleteEuCountryRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = if (!isEuCountryValid()) {
    Some(Redirect(controllers.routes.EuCountryController.onPageLoad(waypoints)))
  } else {
    None
  }

  private def incompleteMoveDateRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = if (!isMoveDateValid()) {
    Some(Redirect(controllers.routes.MoveDateController.onPageLoad(waypoints)))
  } else {
    None
  }

  private def incompleteEuVatNumberRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = if (!isEuVatNumberValid()) {
    Some(Redirect(controllers.routes.EuVatNumberController.onPageLoad(waypoints)))
  } else {
    None
  }
  

  private def incompleteStoppedUsingServiceRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    if (request.userAnswers.get(LeaveSchemePage).contains(true)) {
      if (!hasStoppedUsingServiceDate()) {
        Some(Redirect(controllers.routes.StoppedUsingServiceDateController.onPageLoad(waypoints)))
      } else {
        None
      }
    } else {
      None
    }
  }

  private def noJourneyAnswered()(implicit request: DataRequest[AnyContent]): Option[Result] = {
    if (request.userAnswers.get(MoveCountryPage).contains(true)) {
      None
    } else {
      if (request.userAnswers.get(LeaveSchemePage).contains(true)) {
        None
      } else {
        Some(Redirect(controllers.routes.MoveCountryController.onPageLoad(EmptyWaypoints)))
      }
    }
  }
}
