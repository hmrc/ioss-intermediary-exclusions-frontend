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

package services

import connectors.RegistrationConnector
import connectors.RegistrationConnectorHttpParser.AmendRegistrationResultResponse
import models.amend.{ExclusionAuditModel, ExclusionAuditType, SubmissionResult}
import models.{CountryWithValidationDetails, UserAnswers}
import models.etmp.EtmpMessageType.IOSSIntAmend
import models.etmp.amend.*
import models.etmp.*
import pages.{EuCountryPage, EuVatNumberPage, MoveDatePage, StoppedUsingServiceDatePage}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success


class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector,
                                     auditService: AuditService
                                   )(implicit ec: ExecutionContext) {

  def amendRegistrationAndAudit(
                                 userId: String,
                                 userAnswers: UserAnswers,
                                 intermediaryNumber: String,
                                 displayRegistration: EtmpDisplayRegistration,
                                 exclusionReason: Option[EtmpExclusionReason],
                                 exclusionAuditType: ExclusionAuditType
                               )(implicit hc: HeaderCarrier, request: Request[_]): Future[AmendRegistrationResultResponse] =

    val success: ExclusionAuditModel = ExclusionAuditModel(
      exclusionAuditType = exclusionAuditType,
      userId = userId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      userAnswers = userAnswers,
      intermediaryNumber = intermediaryNumber,
      displayRegistration = displayRegistration,
      exclusionReason = exclusionReason,
      submissionResult = SubmissionResult.Success
    )
    val failure: ExclusionAuditModel = success.copy(submissionResult = SubmissionResult.Failure)

    amendRegistration(userAnswers, exclusionReason, intermediaryNumber, displayRegistration).andThen {
      case Success(Right(_)) => auditService.audit(success)(hc, request)
      case _ => auditService.audit(failure)(hc, request)
    }

  def amendRegistration(
                         answers: UserAnswers,
                         exclusionReason: Option[EtmpExclusionReason],
                         intermediaryNumber: String,
                         registration: EtmpDisplayRegistration
                       )(implicit hc: HeaderCarrier): Future[AmendRegistrationResultResponse] = {

    registrationConnector.amend(buildEtmpAmendRegistrationRequest(
      answers,
      exclusionReason,
      registration,
      intermediaryNumber
    ))
  }

  private def buildEtmpAmendRegistrationRequest(
                                                 answers: UserAnswers,
                                                 exclusionReason: Option[EtmpExclusionReason],
                                                 registration: EtmpDisplayRegistration,
                                                 intermediaryNumber: String
                                               ): EtmpAmendRegistrationRequest = {

    EtmpAmendRegistrationRequest(
      administration = EtmpAdministration(messageType = IOSSIntAmend),
      changeLog = EtmpAmendRegistrationChangeLog(
        tradingNames = false,
        fixedEstablishments = false,
        contactDetails = false,
        bankDetails = false,
        reRegistration = exclusionReason.isEmpty,
        otherAddress = false
      ),
      exclusionDetails = exclusionReason.map(getExclusionDetailsForType(_, answers)),
      customerIdentification = EtmpAmendCustomerIdentification(intermediaryNumber),
      tradingNames = registration.tradingNames,
      intermediaryDetails = registration.intermediaryDetails,
      otherAddress = registration.otherAddress,
      schemeDetails = buildSchemeDetailsFromDisplay(registration.schemeDetails),
      bankDetails = registration.bankDetails,
    )
  }

  private def buildSchemeDetailsFromDisplay(etmpDisplaySchemeDetails: EtmpDisplaySchemeDetails): EtmpSchemeDetails = {
    EtmpSchemeDetails(
      commencementDate = etmpDisplaySchemeDetails.commencementDate,
      euRegistrationDetails = etmpDisplaySchemeDetails.euRegistrationDetails.map(buildEuRegistrationDetails),
      contactName = etmpDisplaySchemeDetails.contactName,
      businessTelephoneNumber = etmpDisplaySchemeDetails.businessTelephoneNumber,
      businessEmailId = etmpDisplaySchemeDetails.businessEmailId,
      nonCompliantReturns = etmpDisplaySchemeDetails.nonCompliantReturns,
      nonCompliantPayments = etmpDisplaySchemeDetails.nonCompliantPayments,
      previousEURegistrationDetails = Seq.empty,
      websites = None
    )
  }

  private def buildEuRegistrationDetails(euDisplayRegistrationDetails: EtmpDisplayEuRegistrationDetails): EtmpEuRegistrationDetails = {
    EtmpEuRegistrationDetails(
      countryOfRegistration = euDisplayRegistrationDetails.issuedBy,
      traderId = buildTraderId(euDisplayRegistrationDetails.vatNumber, euDisplayRegistrationDetails.taxIdentificationNumber),
      tradingName = euDisplayRegistrationDetails.fixedEstablishmentTradingName,
      fixedEstablishmentAddressLine1 = euDisplayRegistrationDetails.fixedEstablishmentAddressLine1,
      fixedEstablishmentAddressLine2 = euDisplayRegistrationDetails.fixedEstablishmentAddressLine2,
      townOrCity = euDisplayRegistrationDetails.townOrCity,
      regionOrState = euDisplayRegistrationDetails.regionOrState,
      postcode = euDisplayRegistrationDetails.postcode
    )
  }

  private def buildTraderId(maybeVatNumber: Option[String], maybeTaxIdentificationNumber: Option[String]): TraderId = {
    (maybeVatNumber, maybeTaxIdentificationNumber) match {
      case (Some(vatNumber), _) => VatNumberTraderId(vatNumber)
      case (_, Some(taxIdentificationNumber)) => TaxRefTraderID(taxIdentificationNumber)
      case _ => throw new IllegalStateException("Neither vat number nor tax id were provided")
    }
  }

  private def getExclusionDetailsForType(exclusionReason: EtmpExclusionReason, answers: UserAnswers): EtmpExclusionDetails = {
    exclusionReason match {
      case EtmpExclusionReason.TransferringMSID => getExclusionDetailsForTransferringMSID(answers)
      case EtmpExclusionReason.VoluntarilyLeaves => getExclusionDetailsForVoluntarilyLeaves(answers)
      case EtmpExclusionReason.Reversal => getExclusionDetailsForReversal
      case _ => throw new Exception("Exclusion reason not valid")
    }
  }

  private def getExclusionDetailsForTransferringMSID(answers: UserAnswers): EtmpExclusionDetails = {

    val country = answers.get(EuCountryPage).getOrElse(throw new Exception("No country provided"))
    val moveDate = answers.get(MoveDatePage).getOrElse(throw new Exception("No move date provided"))
    val euVatNumber = answers.get(EuVatNumberPage).getOrElse(throw new Exception("No Vat Number provided"))
    val convertedVatNumber = CountryWithValidationDetails.convertTaxIdentifierForTransfer(euVatNumber, country.code)

    EtmpExclusionDetails(
      revertExclusion = false,
      noLongerSupplyGoods = false,
      noLongerEligible = false,
      exclusionRequestDate = None,
      identificationValidityDate = None,
      intExclusionRequestDate = Some(LocalDate.now(clock)),
      newMemberState = Some(EtmpNewMemberState(
        newMemberState = true,
        ceaseSpecialSchemeDate = None,
        ceaseFixedEstDate = None,
        movePOBDate = moveDate,
        issuedBy = country.code,
        vatNumber = convertedVatNumber
      ))
    )
  }

  private def getExclusionDetailsForVoluntarilyLeaves(answers: UserAnswers): EtmpExclusionDetails = {

    val stoppedUsingServiceDate = answers.get(StoppedUsingServiceDatePage).getOrElse(throw new Exception("No stopped using service date provided"))

    EtmpExclusionDetails(
      revertExclusion = false,
      noLongerSupplyGoods = false,
      noLongerEligible = false,
      exclusionRequestDate = None,
      identificationValidityDate = None,
      intExclusionRequestDate = Some(stoppedUsingServiceDate),
      newMemberState = None
    )
  }

  private def getExclusionDetailsForReversal: EtmpExclusionDetails = {
    EtmpExclusionDetails(
      revertExclusion = true,
      noLongerSupplyGoods = false,
      noLongerEligible = false,
      exclusionRequestDate = None,
      identificationValidityDate = None,
      intExclusionRequestDate = Some(LocalDate.now(clock)),
      newMemberState = None
    )
  }
}
