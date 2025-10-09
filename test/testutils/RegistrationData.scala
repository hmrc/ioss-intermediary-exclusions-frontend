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

package testutils

import base.SpecBase
import config.Constants.maxTradingNames
import models.etmp.*
import models.etmp.amend.{EtmpAmendCustomerIdentification, EtmpAmendRegistrationChangeLog, EtmpAmendRegistrationRequest}
import models.{Bic, Country, Iban}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.LocalDate

object RegistrationData extends SpecBase {

  val etmpDisplaySchemeDetails: EtmpDisplaySchemeDetails = EtmpDisplaySchemeDetails(
    commencementDate = "2023-01-01",
    euRegistrationDetails = Seq.empty,
    contactName = "John Doe",
    businessTelephoneNumber = "0123456789",
    businessEmailId = "test@example.com",
    unusableStatus = false,
    nonCompliantReturns = None,
    nonCompliantPayments = None
  )

  val genBankDetails: EtmpBankDetails = EtmpBankDetails(
    accountName = arbitrary[String].sample.value,
    bic = Some(arbitrary[Bic].sample.value),
    iban = arbitrary[Iban].sample.value
  )

  val etmpEuRegistrationDetails: EtmpEuRegistrationDetails = EtmpEuRegistrationDetails(
    countryOfRegistration = arbitrary[Country].sample.value.code,
    traderId = arbitraryVatNumberTraderId.arbitrary.sample.value,
    tradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )
  
  val etmpSchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDate.now.format(dateFormatter),
    euRegistrationDetails = Seq(etmpEuRegistrationDetails),
    contactName = arbitrary[String].sample.value,
    businessTelephoneNumber = arbitrary[String].sample.value,
    businessEmailId = arbitrary[String].sample.value,
    nonCompliantReturns = Some(arbitrary[String].sample.value),
    nonCompliantPayments = Some(arbitrary[String].sample.value),
    previousEURegistrationDetails = Seq.empty,
    websites = None
  )

  val etmpRegistrationRequest: EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = arbitrary[EtmpAdministration].sample.value,
    customerIdentification = arbitrary[EtmpCustomerIdentification].sample.value,
    tradingNames = Seq(arbitrary[EtmpTradingName].sample.value),
    schemeDetails = etmpSchemeDetails,
    bankDetails = genBankDetails,
    intermediaryDetails = Some(arbitraryIntermediaryDetails.arbitrary.sample.value),
    otherAddress = Some(arbitrary[EtmpOtherAddress].sample.value)
  )

  val etmpDisplayRegistration: EtmpDisplayRegistration = EtmpDisplayRegistration(
    exclusions = Seq.empty,
    schemeDetails = etmpDisplaySchemeDetails,
    tradingNames = Gen.listOfN(maxTradingNames, arbitraryEtmpTradingName.arbitrary).sample.value,
    intermediaryDetails = etmpRegistrationRequest.intermediaryDetails,
    otherAddress = etmpRegistrationRequest.otherAddress,
    bankDetails = genBankDetails
  )

  val etmpAmendRegistrationRequest: EtmpAmendRegistrationRequest = EtmpAmendRegistrationRequest(
    administration = EtmpAdministration(EtmpMessageType.IOSSIntAmend),
    changeLog = EtmpAmendRegistrationChangeLog(
      tradingNames = true,
      fixedEstablishments = true,
      contactDetails = true,
      bankDetails = true,
      reRegistration = true,
      otherAddress = true
    ),
    exclusionDetails = None,
    customerIdentification = arbitrary[EtmpAmendCustomerIdentification].sample.value,
    tradingNames = Seq(arbitrary[EtmpTradingName].sample.value),
    intermediaryDetails = etmpRegistrationRequest.intermediaryDetails,
    otherAddress = etmpRegistrationRequest.otherAddress,
    schemeDetails = etmpSchemeDetails,
    bankDetails = genBankDetails
  )
  
}
