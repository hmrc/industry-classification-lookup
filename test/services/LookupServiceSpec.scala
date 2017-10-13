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

import com.typesafe.config._
import config.MicroserviceConfig
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
    val sicCodeDescription = Json.parse(
      """
        |{
        |  "currentFRSPercent":"5.0",
        |  "description":"test description",
        |  "frsCategory":"test frs category",
        |  "displayDetails":"test display details"
        |}
      """.stripMargin).as[JsObject]

    "return the sic code description if a matching sic code is found in config" in new Setup {

      val sicCodeConfig: Set[(String, ConfigValue)] = Set(
        "description" -> ConfigValueFactory.fromAnyRef("test description"),
        "displayDetails" -> ConfigValueFactory.fromAnyRef("test display details"),
        "frsCategory" -> ConfigValueFactory.fromAnyRef("test frs category"),
        "currentFRSPercent" -> ConfigValueFactory.fromAnyRef("5.0")
      )

      when(mockConfig.getConfigObject(eqTo(s"sic.codes.$sicCode")))
        .thenReturn(sicCodeConfig)

      val result: Option[JsObject] = service.lookup(sicCode)

      result shouldBe Some(sicCodeDescription)
    }

    "return nothing when a matching sic code is not found in config and an exception is thrown" in new Setup {

      when(mockConfig.getConfigObject(eqTo(s"sic.codes.$sicCode")))
        .thenThrow(new RuntimeException)

      val result: Option[JsObject] = service.lookup(sicCode)

      result shouldBe None
    }
  }

  "configToJson" should {

    "convert a set of config values into json" in new Setup {

      val config: Set[(String, ConfigValue)] = Set(
        "key1" -> ConfigValueFactory.fromAnyRef("value1"),
        "key2" -> ConfigValueFactory.fromAnyRef("value2"),
        "key3" -> ConfigValueFactory.fromAnyRef(3),
        "key4" -> ConfigValueFactory.fromAnyRef(true)
      )

      val expectedJson: JsObject = Json.parse(
        """
          |{
          |  "key1":"value1",
          |  "key2":"value2",
          |  "key3":"3",
          |  "key4":"true"
          |}
        """.stripMargin).as[JsObject]

      service.configToJson(config) shouldBe expectedJson

    }
  }
}
