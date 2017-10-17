/*
 * Copyright 2017 HM Revenue & Customs
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

import config.MicroserviceConfig
import models.SicCode
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}

class LookupServiceSpec extends UnitSpec with MockitoSugar {

  val mockConfig: MicroserviceConfig = mock[MicroserviceConfig]

  trait Setup {
    val service: LookupService = new LookupService {
      val config: MicroserviceConfig = mockConfig
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

      val result: Option[SicCode] = service.lookup(sicCode)

      val expectedResult = SicCode(sicCode, "test description")

      result shouldBe Some(expectedResult)
    }
  }
}
