/*
 * Copyright 2019 HM Revenue & Customs
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

package helpers

import java.util.concurrent.TimeUnit.SECONDS

import akka.util.Timeout
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.Helpers
import uk.gov.hmrc.play.test.UnitSpec

trait ControllerSpec extends UnitSpec with MockitoSugar {

  private val FIVE = 5L
  private implicit val timeout: Timeout = Timeout(FIVE, SECONDS)

  //Use this instead of 'jsonBodyOf' from UnitSpec so an Application is not required for the Materializer
  def bodyAsJson(res: Result): JsValue = Helpers.contentAsJson(res)
}
