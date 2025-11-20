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

package models.amend

import models.UserAnswers
import models.etmp.{EtmpDisplayRegistration, EtmpExclusionReason}
import play.api.libs.json.{JsValue, Json}

case class ExclusionAuditModel(
                              exclusionAuditType: ExclusionAuditType,
                              userId: String,
                              userAgent: String,
                              userAnswers: UserAnswers,
                              intermediaryNumber: String,
                              displayRegistration: EtmpDisplayRegistration,
                              exclusionReason: Option[EtmpExclusionReason],
                              submissionResult: SubmissionResult
                              ) extends JsonAuditModel {

  override def auditType: String = exclusionAuditType.auditType

  override def transactionName: String = exclusionAuditType.transactionName

  override def detail: JsValue = Json.obj(
    "userId" -> userId,
    "browserUserAgent" -> userAgent,
    "userAnswersDetails" -> Json.toJson(userAnswers),
    "intermediaryNumber" -> intermediaryNumber,
    "registration" -> Json.toJson(displayRegistration),
    "exclusionReason" -> Json.toJson(exclusionReason),
    "submissionResult" -> submissionResult
  )
}
