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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class EtmpDisplayRegistrationSpec extends SpecBase {

  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

  "EtmpDisplayRegistration" - {

    "must serialise to EtmpDisplayRegistration" in {

      val json = Json.obj(
        "exclusions" -> etmpDisplayRegistration.exclusions,
        "schemeDetails" -> etmpDisplayRegistration.schemeDetails,
        "tradingNames" -> etmpDisplayRegistration.tradingNames,
        "intermediaryDetails" -> etmpDisplayRegistration.intermediaryDetails,
        "otherAddress" -> etmpDisplayRegistration.otherAddress,
        "bankDetails" -> etmpDisplayRegistration.bankDetails
      )

      val expectedResult = EtmpDisplayRegistration(
        exclusions = etmpDisplayRegistration.exclusions,
        schemeDetails = etmpDisplayRegistration.schemeDetails,
        tradingNames = etmpDisplayRegistration.tradingNames,
        intermediaryDetails = etmpDisplayRegistration.intermediaryDetails,
        otherAddress = etmpDisplayRegistration.otherAddress,
        bankDetails = etmpDisplayRegistration.bankDetails
      )

      json.validate[EtmpDisplayRegistration] `mustBe` JsSuccess(expectedResult)
    }

    "must deserialise from EtmpDisplayRegistration" in {

      val fromEtmpDisplayRegistration = EtmpDisplayRegistration(
        exclusions = etmpDisplayRegistration.exclusions,
        schemeDetails = etmpDisplayRegistration.schemeDetails,
        tradingNames = etmpDisplayRegistration.tradingNames,
        intermediaryDetails = etmpDisplayRegistration.intermediaryDetails,
        otherAddress = etmpDisplayRegistration.otherAddress,
        bankDetails = etmpDisplayRegistration.bankDetails
      )

      val expectedResult = Json.obj(
        "exclusions" -> etmpDisplayRegistration.exclusions,
        "schemeDetails" -> etmpDisplayRegistration.schemeDetails,
        "tradingNames" -> etmpDisplayRegistration.tradingNames,
        "intermediaryDetails" -> etmpDisplayRegistration.intermediaryDetails,
        "otherAddress" -> etmpDisplayRegistration.otherAddress,
        "bankDetails" -> etmpDisplayRegistration.bankDetails
      )

      Json.toJson(fromEtmpDisplayRegistration) `mustBe` expectedResult
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpDisplayRegistration] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "exclusions" -> 123456
      )

      json.validate[EtmpDisplayRegistration] `mustBe` a[JsError]
    }
  }
}
