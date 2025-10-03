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
import models.etmp.EtmpExclusionReason.Reversal
import models.etmp.{EtmpDisplayRegistration, EtmpExclusion, EtmpExclusionReason}
import models.requests.OptionalDataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.CheckExclusionsService

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckIntermediaryExcludedFilterSpec extends SpecBase {

  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

  private val mockCheckExclusionsService: CheckExclusionsService = mock[CheckExclusionsService]

  class Harness() extends CheckIntermediaryExcludedFilterImpl(mockCheckExclusionsService) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return None when there are no exclusions present" in {

      val nonExcludedIntermediaryRegistration = etmpDisplayRegistration
        .copy(exclusions = Seq.empty)

      when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn None

      val application = applicationBuilder()
        .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
        .build()

      running(application) {
        val request = OptionalDataRequest(FakeRequest(), userAnswersId, None, intermediaryNumber, nonExcludedIntermediaryRegistration)

        val controller = new Harness()

        val result = controller.callFilter(request).futureValue

        result must not be defined
        verify(mockCheckExclusionsService, times(1)).getLastExclusionWithoutReversal(eqTo(nonExcludedIntermediaryRegistration.exclusions.toList))
      }
    }

    EtmpExclusionReason.values.filterNot(_.eq(Reversal)).foreach { exclusionReason =>
      s"must redirect to Access Denied Page when there is an exclusion present for exclusion reason: $exclusionReason" in {

        val effectiveDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate)

        val etmpExclusion: EtmpExclusion = EtmpExclusion(
          exclusionReason = exclusionReason,
          effectiveDate = effectiveDate,
          decisionDate = LocalDate.now(stubClockAtArbitraryDate),
          quarantine = false
        )
        val excludedIntermediaryRegistration = etmpDisplayRegistration
          .copy(exclusions = Seq(etmpExclusion))

        when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn Some(List(etmpExclusion))

        val application = applicationBuilder()
          .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
          .build()

        running(application) {
          val request = OptionalDataRequest(FakeRequest(), userAnswersId, None, intermediaryNumber, excludedIntermediaryRegistration)

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result.value `mustBe` Redirect(routes.AccessDeniedExcludedController.onPageLoad())
          verify(mockCheckExclusionsService, times(1)).getLastExclusionWithoutReversal(eqTo(Seq(etmpExclusion).toList))
        }
      }
    }

    "must return None when there is an exclusion present for exclusion reason Reversal" in {

      val effectiveDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate)

      val etmpExclusion: EtmpExclusion = EtmpExclusion(
        exclusionReason = Reversal,
        effectiveDate = effectiveDate,
        decisionDate = LocalDate.now(stubClockAtArbitraryDate),
        quarantine = false
      )
      val excludedIntermediaryRegistration = etmpDisplayRegistration
        .copy(exclusions = Seq(etmpExclusion))

      when(mockCheckExclusionsService.getLastExclusionWithoutReversal(any())) thenReturn Some(List(etmpExclusion))

      val application = applicationBuilder()
        .overrides(bind[CheckExclusionsService].toInstance(mockCheckExclusionsService))
        .build()

      running(application) {
        val request = OptionalDataRequest(FakeRequest(), userAnswersId, None, intermediaryNumber, excludedIntermediaryRegistration)

        val controller = new Harness()

        val result = controller.callFilter(request).futureValue

        result.value `mustBe` Redirect(routes.AccessDeniedExcludedController.onPageLoad())
        verify(mockCheckExclusionsService, times(1)).getLastExclusionWithoutReversal(eqTo(Seq(etmpExclusion).toList))
      }
    }
  }
}
