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

import base.SpecBase
import models.etmp.EtmpExclusionReason.Reversal
import models.etmp.{EtmpExclusion, EtmpExclusionReason}

class CheckExclusionsServiceSpec extends SpecBase {

  private val etmpExclusion: EtmpExclusion = arbitraryEtmpExclusion.arbitrary.sample.value

  "Check Exclusions Service" - {

    ".getLastExclusionWithoutReversal" - {

      EtmpExclusionReason.values.filterNot(_.eq(Reversal)).foreach { exclusionReason =>
        s"must return an Option[EtmpExclusion] when one exists and the exclusion reason is: $exclusionReason" in {

          val updatedEtmpExclusion: EtmpExclusion = etmpExclusion
            .copy(exclusionReason = exclusionReason)

          val service = new CheckExclusionsService

          val result = service.getLastExclusionWithoutReversal(List(updatedEtmpExclusion))

          result `mustBe` Some(updatedEtmpExclusion)
        }
      }

      "must return None when an exclusion exists with the exclusion reason: Reversal" in {

        val updatedEtmpExclusion: EtmpExclusion = etmpExclusion
          .copy(exclusionReason = Reversal)

        val service = new CheckExclusionsService

        val result = service.getLastExclusionWithoutReversal(List(updatedEtmpExclusion))

        result `mustBe` None
      }

      "must return None when there are no exclusion present" in {

        val service = new CheckExclusionsService

        val result = service.getLastExclusionWithoutReversal(List.empty)

        result `mustBe` None
      }
    }
  }
}
