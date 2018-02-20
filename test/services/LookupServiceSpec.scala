/*
 * Copyright 2018 HM Revenue & Customs
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

import config.ICLConfig
import connectors.IndexConnector
import models.SicCode
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import services.Indexes._
import uk.gov.hmrc.play.test.UnitSpec

class LookupServiceSpec extends UnitSpec with MockitoSugar {

  val mockConfig: ICLConfig = mock[ICLConfig]
  val mockIndex: IndexConnector = mock[IndexConnector]

  trait Setup {
    val service: LookupService = new LookupService {
      val config: ICLConfig = mockConfig
      val indexes = Map(HMRC_SIC8_INDEX -> mockIndex)
    }
  }

  "lookup" should {

    val sicCode = "12345678"
    val sicCodeLookupResult = Json.parse(
      s"""
        |{
        | "sectors":[
        |   {
        |     "sics":[
        |       {
        |         "code":"$sicCode",
        |         "desc":"test description"
        |       }
        |     ]
        |   }
        | ]
        |}
      """.stripMargin).as[JsObject]

    "return the sic code description if a matching sic code is found in config" in new Setup {

      when(mockConfig.getConfigObject(eqTo("sic")))
        .thenReturn(sicCodeLookupResult)

      when(mockIndex.lookup(eqTo(sicCode))).thenReturn(Some(SicCode(sicCode, "test description")))

      val result: Option[SicCode] = service.lookup(sicCode, HMRC_SIC8_INDEX)

      val expectedResult = SicCode(sicCode, "test description")

      result shouldBe Some(expectedResult)
    }
  }

  "search" should {

    "return the results of the index query" in new Setup {
      val query = "Foo"
      val result = SearchResult(1, 1, Seq(SicCode("12345", "test description")), Seq())
      when(mockIndex.search(eqTo(query), any[Int], any[Int], any(), any())).thenReturn(result)

      service.search(query, HMRC_SIC8_INDEX) shouldBe result
    }
  }
}
