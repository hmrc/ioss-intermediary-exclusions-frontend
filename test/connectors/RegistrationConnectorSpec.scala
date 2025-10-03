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

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.etmp.EtmpDisplayRegistration
import models.responses.{InternalServerError, InvalidJson}
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = applicationBuilder()
    .configure(
      "microservice.services.ioss-intermediary-registration.port" -> server.port
    )
    .build()

  "RegistrationConnector" - {

    "getDisplayRegistration" - {

      val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

      val getDisplayRegistrationUrl: String = s"/ioss-intermediary-registration/get-registration/$intermediaryNumber"

      "must return Right(EtmpDisplayRegistration) when one exists for the given intermediary number" in {

        val responseJson = Json.toJson(etmpDisplayRegistration).toString

        val json =
          s"""{
             |  "etmpDisplayRegistration": $responseJson
             |}""".stripMargin

        server.stubFor(
          get(urlEqualTo(getDisplayRegistrationUrl))
            .willReturn(ok
              .withBody(json)
            )
        )

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.getDisplayRegistration(intermediaryNumber).futureValue

          result `mustBe` Right(etmpDisplayRegistration)
        }
      }

      "must return Left(InvalidJson) when server returns OK but JSON cannot be parsed correctly" in {

        val invalidJson =
          s"""{
             |  "etmpDisplayRegistration": {
             |    "exclusions": 12345
             |  }
             |}""".stripMargin

        server.stubFor(
          get(urlEqualTo(getDisplayRegistrationUrl))
            .willReturn(ok
              .withBody(invalidJson)
            )
        )

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.getDisplayRegistration(intermediaryNumber).futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      "must return Left(InternalServerError) when server responds with an error" in {

        server.stubFor(
          get(urlEqualTo(getDisplayRegistrationUrl))
            .willReturn(serverError())
        )

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.getDisplayRegistration(intermediaryNumber).futureValue

          result `mustBe` Left(InternalServerError)
        }
      }
    }
  }
}
