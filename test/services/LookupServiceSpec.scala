/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.utils.IndexConnector
import connectors.{GDSRegisterSIC5IndexConnector, ONSSupplementSIC5IndexConnector}
import models.SicCode
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import services.Indexes._

class LookupServiceSpec extends PlaySpec with MockitoSugar {

  val lang: String = "en"
  val mockConfig: ICLConfig = mock[ICLConfig]
  val mockONSIndex: ONSSupplementSIC5IndexConnector = mock[ONSSupplementSIC5IndexConnector]
  val mockGDSIndex: GDSRegisterSIC5IndexConnector = mock[GDSRegisterSIC5IndexConnector]

  trait Setup {
    val service: LookupService = new LookupService(mockGDSIndex, mockONSIndex) {
      override val indexes: Map[String, IndexConnector] =
        Map(ONS_SUPPLEMENT_SIC5_INDEX -> mockONSIndex, GDS_REGISTER_SIC5_INDEX -> mockGDSIndex)
    }
  }

  "lookup" should {
    val sicCode = "12345678"
    val sicCodeResult = SicCode(sicCode, "test description", "test welsh description")
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

    "return an Empty list" in new Setup {
      val searchList: List[String] = List("invalid", "invalid2")

      when(mockConfig.getConfigObject(eqTo("sic")))
        .thenReturn(sicCodeLookupResult)

      when(mockGDSIndex.lookup(any())).thenReturn(
        None,
        None
      )

      service.lookup(searchList) mustBe List.empty[SicCode]
    }

    "return a list of flattened codes (all matching)" in new Setup {
      val searchList: List[String] = List(sicCode, sicCode)

      when(mockConfig.getConfigObject(eqTo("sic")))
        .thenReturn(sicCodeLookupResult)

      when(mockGDSIndex.lookup(any())).thenReturn(Some(sicCodeResult), Some(sicCodeResult))

      service.lookup(searchList) mustBe List(sicCodeResult, sicCodeResult)
    }

    "return a list of flattened codes (single result passed in)" in new Setup {
      val searchList: List[String] = List(sicCode)

      when(mockConfig.getConfigObject(eqTo("sic")))
        .thenReturn(sicCodeLookupResult)

      when(mockGDSIndex.lookup(any())).thenReturn(Some(sicCodeResult))

      service.lookup(searchList) mustBe List(sicCodeResult)
    }

    "return a list of flattened codes (some matching)" in new Setup {
      val searchList: List[String] = List(sicCode, "invalid")

      when(mockConfig.getConfigObject(eqTo("sic")))
        .thenReturn(sicCodeLookupResult)

      when(mockGDSIndex.lookup(any())).thenReturn(Some(sicCodeResult), None)

      service.lookup(searchList) mustBe List(sicCodeResult)
    }
  }

  "search" should {

    "return the results of the index query" in new Setup {
      val query = "Foo"
      val result: SearchResult = SearchResult(1, 1, Seq(SicCode("12345", "test description", "test welsh description")), Seq())
      when(mockONSIndex.search(eqTo(query), any[Int](), any[Int](), any(), any(), eqTo(false), eqTo(lang))).thenReturn(result)

      service.search(query, ONS_SUPPLEMENT_SIC5_INDEX, lang = lang) mustBe result
    }
  }
}
