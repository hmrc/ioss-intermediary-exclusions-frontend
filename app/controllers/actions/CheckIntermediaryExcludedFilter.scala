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

package controllers.actions

import logging.Logging
import models.requests.OptionalDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.CheckExclusionsService
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckIntermediaryExcludedFilterImpl @Inject()(
                                                     checkExclusionsService: CheckExclusionsService
                                                   )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] with Logging {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {

    val maybeEtmpExclusion: Boolean = checkExclusionsService
      .getLastExclusionWithoutReversal(request.displayRegistration.exclusions.toList).isDefined

    if (maybeEtmpExclusion) {
      Some(Redirect(controllers.routes.AccessDeniedExcludedController.onPageLoad().url)).toFuture
    } else {
      None.toFuture
    }
  }
}

class CheckIntermediaryExcludedFilter @Inject()(
                                                 checkExclusionsService: CheckExclusionsService
                                               )(implicit ec: ExecutionContext) {

  def apply(): CheckIntermediaryExcludedFilterImpl =
    new CheckIntermediaryExcludedFilterImpl(checkExclusionsService)
}
