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

package date

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters.{firstDayOfNextMonth, lastDayOfMonth}
import java.time.{Clock, LocalDate, ZoneOffset}
import javax.inject.Inject

class Dates @Inject()(val today: Today) {

  val MoveDayOfMonthSplit: Int = 10
  private val StopDayOfMonthSplit: Int = 15

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  private val digitsFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MM yyyy")

  def dateHint: String = digitsFormatter.format(today.date)

  def minMoveDate: LocalDate =
    (if (today.date.getDayOfMonth <= MoveDayOfMonthSplit) today.date.minusMonths(1) else today.date)
      .withDayOfMonth(1)

  def maxMoveDate: LocalDate =
    today.date.plusMonths(1).withDayOfMonth(MoveDayOfMonthSplit)

  def getLeaveDateWhenStoppedUsingService(exclusionDate: LocalDate): LocalDate = {
    val lastDayOfTheMonth = today.date.`with`(lastDayOfMonth())
    val firstDayOfTheNextMonth = today.date.`with`(firstDayOfNextMonth())

    if (exclusionDate <= lastDayOfTheMonth.minusDays(StopDayOfMonthSplit)) {
      firstDayOfTheNextMonth
    } else {
      firstDayOfTheNextMonth.plusMonths(1)
    }
  }
  def getLeaveDateWhenStoppedSellingGoods: LocalDate = {
    today.date.`with`(firstDayOfNextMonth())
  }
}

object Dates {
  val clock: Clock = Clock.systemDefaultZone.withZone(ZoneOffset.UTC)
}
