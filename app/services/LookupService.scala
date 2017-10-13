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

import com.typesafe.config.ConfigValue
import config.MicroserviceConfig
import play.api.Logger
import play.api.libs.json.{JsObject, Json}

import scala.util.{Failure, Success, Try}

@Singleton
class LookupServiceImpl @Inject()(val config: MicroserviceConfig) extends LookupService

trait LookupService {

  protected val config: MicroserviceConfig

  def lookup(sicCode: String): Option[JsObject] = {
    Try(config.getConfigObject(s"sic.codes.$sicCode")) match {
      case Success(sicCodeConfig) => Some(configToJson(sicCodeConfig))
      case Failure(ex) =>
        Logger.error(s"[Lookup] Couldn't find sic code $sicCode in config", ex)
        None
    }
  }

  private[services] def configToJson(set: Set[(String, ConfigValue)]): JsObject = {
    set.foldLeft(Json.obj())((json, config) => json ++ Json.obj(config._1 -> config._2.unwrapped.toString))
  }
}
