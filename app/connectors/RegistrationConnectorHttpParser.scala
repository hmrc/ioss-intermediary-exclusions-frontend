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
import models.etmp.EtmpDisplayRegistration
import models.responses.{ErrorResponse, InternalServerError, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object RegistrationConnectorHttpParser extends Logging {

  type EtmpDisplayRegistrationResponse = Either[ErrorResponse, EtmpDisplayRegistration]
  type AmendRegistrationResultResponse = Either[ErrorResponse, Any]

  implicit object EtmpDisplayRegistrationResponseReads extends HttpReads[EtmpDisplayRegistrationResponse] {

    override def read(method: String, url: String, response: HttpResponse): EtmpDisplayRegistrationResponse = {
      response.status match {
        case OK =>
          (response.json \ "etmpDisplayRegistration").validate[EtmpDisplayRegistration] match {
            case JsSuccess(etmpDisplayRegistration: EtmpDisplayRegistration, _) => Right(etmpDisplayRegistration)
            case JsError(errors) =>
              logger.error(s"Failed trying to parse ETMP Display Registration JSON with response body: ${response.body}" +
                s" and status: ${response.status} with errors: $errors.")
              Left(InvalidJson)
          }

        case status =>
          logger.error(s"An unknown error occurred when trying to retrieve ETMP Display Registration with status: $status " +
            s"and response body: ${response.body}")
          Left(InternalServerError)
      }
    }
  }

  implicit object AmendRegistrationResultResponseReads extends HttpReads[AmendRegistrationResultResponse] {
    override def read(method: String, url: String, response: HttpResponse): AmendRegistrationResultResponse = {
      response.status match {
        case OK => Right(())
        case status =>
          Left(UnexpectedResponseStatus(response.status, s"Unexpected amend response, status $status returned"))
      }
    }
  }
}
