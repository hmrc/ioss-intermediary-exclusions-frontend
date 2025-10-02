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

import base.SpecBase
import controllers.routes
import models.etmp.EtmpExclusionReason.{CeasedTrade, FailsToComply, NoLongerMeetsConditions, NoLongerSupplies, Reversal, TransferringMSID, VoluntarilyLeaves}
import models.etmp.{EtmpDisplayRegistration, EtmpExclusion}
import models.requests.OptionalDataRequest
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckIntermediaryExcludedFilterSpec extends SpecBase {

  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

  class Harness() extends CheckIntermediaryExcludedFilterImpl(clock = stubClockAtArbitraryDate) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return None when there are no exclusions present" in {

      val nonExcludedIntermediaryRegistration = etmpDisplayRegistration
        .copy(exclusions = Seq.empty)

      val application = applicationBuilder().build()

      running(application) {
        val request = OptionalDataRequest(FakeRequest(), userAnswersId, None, nonExcludedIntermediaryRegistration)

        val controller = new Harness()

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    Seq(NoLongerSupplies, VoluntarilyLeaves, TransferringMSID).foreach { exclusionReason =>

      s"must return None when there is an exclusion present with exclusion reason $exclusionReason " +
        s"and effective date is before today" in {

        val effectiveDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).plusDays(1)

        val etmpExclusion: EtmpExclusion = EtmpExclusion(
          exclusionReason = exclusionReason,
          effectiveDate = effectiveDate,
          decisionDate = LocalDate.now(stubClockAtArbitraryDate),
          quarantine = false
        )
        val nonExcludedIntermediaryRegistration = etmpDisplayRegistration
          .copy(exclusions = Seq(etmpExclusion))

        val application = applicationBuilder().build()

        running(application) {
          val request = OptionalDataRequest(FakeRequest(), userAnswersId, None, nonExcludedIntermediaryRegistration)

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      s"must redirect to Access Denied Page when there is an exclusion present with exclusion reason $exclusionReason " +
        s"and effective date is on or after today" in {

        val effectiveDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate)

        val etmpExclusion: EtmpExclusion = EtmpExclusion(
          exclusionReason = exclusionReason,
          effectiveDate = effectiveDate,
          decisionDate = LocalDate.now(stubClockAtArbitraryDate),
          quarantine = false
        )
        val excludedIntermediaryRegistration = etmpDisplayRegistration
          .copy(exclusions = Seq(etmpExclusion))

        val application = applicationBuilder().build()

        running(application) {
          val request = OptionalDataRequest(FakeRequest(), userAnswersId, None, excludedIntermediaryRegistration)

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result.value `mustBe` Redirect(routes.AccessDeniedExcludedController.onPageLoad())
        }
      }
    }

    Seq(Reversal, CeasedTrade, NoLongerMeetsConditions, FailsToComply).foreach { exclusionReason =>

      s"must redirect to Access Denied Page when there is an exclusion present but exclusion reason is $exclusionReason" in {

        val effectiveDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate)

        val etmpExclusion: EtmpExclusion = EtmpExclusion(
          exclusionReason = exclusionReason,
          effectiveDate = effectiveDate,
          decisionDate = LocalDate.now(stubClockAtArbitraryDate),
          quarantine = false
        )
        val excludedIntermediaryRegistration = etmpDisplayRegistration
          .copy(exclusions = Seq(etmpExclusion))

        val application = applicationBuilder().build()

        running(application) {
          val request = OptionalDataRequest(FakeRequest(), userAnswersId, None, excludedIntermediaryRegistration)

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result.value `mustBe` Redirect(routes.AccessDeniedExcludedController.onPageLoad())
        }
      }
    }
  }
}
