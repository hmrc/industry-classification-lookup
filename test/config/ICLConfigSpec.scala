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

package config

import com.typesafe.config.ConfigObject
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.ConfigLoader.configObjectLoader
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class ICLConfigSpec extends PlaySpec with MockitoSugar {
  val mockConfig: Configuration = mock[Configuration]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  trait Setup {
    val msConfig: ICLConfig = new ICLConfig(mockServicesConfig, mockConfig)
  }

  "getConfigObject" should {

    val key = "test"

    val map: Map[String, Any] = Map(
      key -> Map(
        "some" -> "thing",
        "nested" -> Map(
          "nestedKey" -> "nestedValue"
        )
      )
    )

    val confObject = Configuration.from(map).getOptional[ConfigObject](key)

    "return a config object as json given a key" in new Setup {

      when(mockConfig.getOptional[ConfigObject](eqTo(key))(eqTo(configObjectLoader))).thenReturn(confObject)

      val retrievedConfig: JsValue = msConfig.getConfigObject(key)

      val expectedJson: JsValue = Json.parse(
        """
          |{
          | "some":"thing",
          | "nested":{
          |   "nestedKey":"nestedValue"
          | }
          |}
        """.stripMargin)

      retrievedConfig mustBe expectedJson
    }

    "throw an unchecked exception when the config object isn't found using the supplied key" in new Setup {

      when(mockConfig.getOptional[ConfigObject](eqTo(key))(eqTo(configObjectLoader))).thenReturn(None)

      val ex: Throwable = intercept[Throwable](msConfig.getConfigObject(key))

      ex.getMessage mustBe s"[Config] Could not find config key $key"
    }
  }
}
