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

package forms

import date.{Dates, LocalDateOps, Today, TodayImpl}
import forms.behaviours.DateBehaviours
import org.mockito.Mockito.when
import org.scalacheck.Gen
import play.api.data.FormError
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class MoveDateFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()

  val mockToday: Today = mock[Today]

  ".value" - {
    val dates = new Dates(new TodayImpl(Dates.clock))
    val form = new MoveDateFormProvider(dates)()

    val minDate: LocalDate = dates.today.date

    val validData: Gen[LocalDate] = datesBetween(
      min = minDate,
      max = minDate.plusMonths(1).withDayOfMonth(10)
    )

    behave like dateField(form, "value", validData)

    behave like mandatoryDateField(form, "value", "moveDate.error.required.all")

    "bind date if today is the 10th of the month or earlier AND " +
      "the form's date is in the previous month or the current month or up to the 10th of the following month" in {
      val todayGen: Gen[LocalDate] = datesBetween(
        min = LocalDate.of(2023, 12, 1),
        max = LocalDate.of(2023, 12, 10)
      )

      val validDatesGen: Gen[LocalDate] = datesBetween(
        min = LocalDate.of(2023, 11, 1),
        max = LocalDate.of(2024, 1, 10)
      )

      forAll(todayGen, validDatesGen) { (today, validDate) =>
        when(mockToday.date).thenReturn(today)
        val dates = new Dates(mockToday)
        val form = new MoveDateFormProvider(dates)()

        val data = formData(validDate)
        val result = form.bind(data)
        result.value.value mustEqual validDate
        result.errors mustBe empty
      }
    }

    "fail to bind date if today is the 10th of the month or earlier AND the form's date is out of range" in {
      val validMinDate: LocalDate = LocalDate.of(2023, 11, 1)
      val validMaxDate: LocalDate = LocalDate.of(2024, 1, 10)

      val todayGen: Gen[LocalDate] = datesBetween(
        min = LocalDate.of(2023, 12, 1),
        max = LocalDate.of(2023, 12, 10)
      )

      val invalidEarlyDatesGen: Gen[LocalDate] = datesBetween(
        min = LocalDate.of(2020, 1, 1),
        max = validMinDate.minusDays(1)
      )

      val invalidLateDatesGen: Gen[LocalDate] = datesBetween(
        min = validMaxDate.plusDays(1),
        max = LocalDate.of(2027, 1, 1)
      )

      val invalidDatesGen: Gen[LocalDate] = Gen.oneOf(invalidEarlyDatesGen, invalidLateDatesGen)

      forAll(todayGen, invalidDatesGen) { (today, invalidDate) =>
        when(mockToday.date).thenReturn(today)
        val dates = new Dates(mockToday)
        val form = new MoveDateFormProvider(dates)()

        val data = formData(invalidDate)
        val result = form.bind(data)
        val formError = if (invalidDate < today) {
          FormError("value", "moveDate.error.invalid.minDate", Seq(dates.formatter.format(validMinDate)))
        } else {
          FormError("value", "moveDate.error.invalid.maxDate", Seq(dates.formatter.format(validMaxDate)))
        }
        result.errors must contain only formError
      }
    }

    "bind date if today is after the 10th of the month AND " +
      "the form's date is in the current month or up to the 10th of the following month" in {
      val todayGen: Gen[LocalDate] = datesBetween(
        min = LocalDate.of(2023, 12, 11),
        max = LocalDate.of(2023, 12, 31)
      )

      val validDatesGen: Gen[LocalDate] = datesBetween(
        min = LocalDate.of(2023, 12, 1),
        max = LocalDate.of(2024, 1, 10)
      )

      forAll(todayGen, validDatesGen) { (today, validDate) =>
        when(mockToday.date).thenReturn(today)
        val dates = new Dates(mockToday)
        val form = new MoveDateFormProvider(dates)()

        val data = formData(validDate)
        val result = form.bind(data)
        result.value.value mustEqual validDate
        result.errors mustBe empty
      }
    }

    "fail to bind date if today is after the 10th of the month AND the form's date is out of range" in {
      val validMinDate: LocalDate = LocalDate.of(2023, 12, 1)
      val validMaxDate: LocalDate = LocalDate.of(2024, 1, 10)

      val todayGen: Gen[LocalDate] = datesBetween(
        min = LocalDate.of(2023, 12, 11),
        max = LocalDate.of(2023, 12, 31)
      )

      val invalidEarlyDatesGen: Gen[LocalDate] = datesBetween(
        min = LocalDate.of(2020, 1, 1),
        max = validMinDate.minusDays(1)
      )

      val invalidLateDatesGen: Gen[LocalDate] = datesBetween(
        min = validMaxDate.plusDays(1),
        max = LocalDate.of(2027, 1, 1)
      )

      val invalidDatesGen: Gen[LocalDate] = Gen.oneOf(invalidEarlyDatesGen, invalidLateDatesGen)

      forAll(todayGen, invalidDatesGen) { (today, invalidDate) =>
        when(mockToday.date).thenReturn(today)
        val dates = new Dates(mockToday)
        val form = new MoveDateFormProvider(dates)()

        val data = formData(invalidDate)
        val result = form.bind(data)
        val formError = if (invalidDate < today) {
          FormError("value", "moveDate.error.invalid.minDate", Seq(dates.formatter.format(validMinDate)))
        } else {
          FormError("value", "moveDate.error.invalid.maxDate", Seq(dates.formatter.format(validMaxDate)))
        }
        result.errors must contain only formError
      }
    }
  }

  def formData(date: LocalDate): Map[String, String] = Map(
    "value.day" -> date.getDayOfMonth.toString,
    "value.month" -> date.getMonthValue.toString,
    "value.year" -> date.getYear.toString
  )
}
