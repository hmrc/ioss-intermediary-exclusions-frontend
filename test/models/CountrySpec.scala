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

package models

import base.SpecBase
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json

class CountrySpec extends SpecBase with ScalaFutures {

  "Country" - {

    "CountryWithValidationDetails" - {

      "must serialize and deserialize correctly" in {
        val country = Country("FR", "France")
        val countryWithValidationDetails = CountryWithValidationDetails(
          country,
          """^FR[A-Z0-9]{2}[0-9]{9}$""",
          "the 11 characters",
          "XX123456789"
        )

        val expectedJson = Json.obj(
          "country" -> Json.obj(
            "code" -> "FR",
            "name" -> "France"
          ),
          "vrnRegex" -> """^FR[A-Z0-9]{2}[0-9]{9}$""",
          "messageInput" -> "the 11 characters",
          "exampleVrn" -> "XX123456789"
        )

        val serializedJson = Json.obj(
          "country" -> Json.toJson(country),
          "vrnRegex" -> countryWithValidationDetails.vrnRegex,
          "messageInput" -> countryWithValidationDetails.messageInput,
          "exampleVrn" -> countryWithValidationDetails.exampleVrn
        )

        serializedJson mustBe expectedJson
      }

      "must validate VRNs using regex for each country" in {
        val testCases = Seq(
          ("AT", "ATU12345678", """^ATU[0-9]{8}$""", true),
          ("AT", "12345678", """^ATU[0-9]{8}$""", false),
          ("FR", "FRXX123456789", """^FR[A-Z0-9]{2}[0-9]{9}$""", true),
          ("FR", "123456789", """^FR[A-Z0-9]{2}[0-9]{9}$""", false),
          ("DE", "DE123456789", """^DE[0-9]{9}$""", true),
          ("DE", "123456789", """^DE[0-9]{9}$""", false)
        )

        testCases.foreach { case (code, vrn, regex, isValid) =>
          val country = Country(code, code)
          val countryValidationDetails = CountryWithValidationDetails(
            country,
            regex,
            "dummy message",
            "dummy example"
          )

          vrn.matches(countryValidationDetails.vrnRegex) mustBe isValid
        }
      }

      "convertTaxIdentifierForTransfer" - {

        "must return identifier without the country code when valid" in {
          val result = CountryWithValidationDetails.convertTaxIdentifierForTransfer("ATU12345678", "AT")
          result mustBe "U12345678"
        }

        "must return identifier without country code prefix when valid" in {
          val result = CountryWithValidationDetails.convertTaxIdentifierForTransfer("ATU12345678", "AT")
          result mustBe "U12345678"
        }

        "must return the original identifier when invalid" in {
          val result = CountryWithValidationDetails.convertTaxIdentifierForTransfer("INVALID123", "AT")
          result mustBe "INVALID123"
        }

        "must throw an exception when the country code is not found" in {
          val exception = intercept[IllegalStateException] {
            CountryWithValidationDetails.convertTaxIdentifierForTransfer("U12345678", "XX")
          }
          exception.getMessage mustBe "Error occurred while getting country code regex, unable to convert identifier"
        }
      }
    }
  }
}
