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

import javax.inject.{Inject, Singleton}

import config.MicroserviceConfig
import models.SicCode
import play.api.libs.json._

@Singleton
class LookupServiceImpl @Inject()(val config: MicroserviceConfig) extends LookupService

trait LookupService {

  protected val config: MicroserviceConfig

  private[services] def fetchSicCodes: JsObject = config.getConfigObject("sic").as[JsObject]

  //todo: refactor to handle list of sic codes
  def lookup(sicCode: String): Option[SicCode] = {
    (fetchSicCodes \ "sectors").as[JsArray].value.flatMap{ sector =>
      (sector \ "sics").as[JsArray].value.find{ sic =>
        (sic \ "code").as[String] == sicCode
      }
    }
  }.headOption.map(_.as[SicCode])
}
