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

package connectors

import logging.Logging
import models.etmp.RegistrationWrapper
import models.responses.{ErrorResponse, InternalServerError, InvalidJson}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object RegistrationHttpParser extends Logging {

  type EtmpDisplayRegistrationResponseWithWrapper = Either[ErrorResponse, RegistrationWrapper]

  implicit object EtmpDisplayRegistrationResponseWithWrapperReads extends HttpReads[EtmpDisplayRegistrationResponseWithWrapper] {

    override def read(method: String, url: String, response: HttpResponse): EtmpDisplayRegistrationResponseWithWrapper = {
      response.status match {
        case OK =>
          response.json.validate[RegistrationWrapper] match {
            case JsSuccess(registrationWrapper, _) => Right(registrationWrapper)
            case JsError(errors) =>
              logger.error(s"Failed trying to parse Registration Wrapper JSON with status: ${response.status} " +
                s"and response body: ${response.body} with errors: $errors.")
              Left(InvalidJson)
          }

        case status =>
          logger.error(s"An unknown error occurred when trying to retrieve Registration Wrapper with status: $status " +
            s"and response body: ${response.body}")
          Left(InternalServerError)
      }
    }
  }
}
