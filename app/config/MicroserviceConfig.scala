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

package config

import javax.inject.{Inject, Singleton}

import com.typesafe.config.ConfigRenderOptions
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}

@Singleton
class MicroserviceConfigImpl @Inject()(val playConfig: Configuration) extends MicroserviceConfig

trait MicroserviceConfig {

  protected val playConfig: Configuration

  def getConfigString(key: String): String = playConfig.getString(key).getOrElse(throw ConfigNotFoundException(key))

  def getConfigObject(key: String): JsValue = playConfig.getObject(key).map{ obj =>
    val jsonValid = obj.render(ConfigRenderOptions.concise())
    Json.parse(jsonValid)
  }.getOrElse(throw ConfigNotFoundException(key))

  private case class ConfigNotFoundException(key: String) extends Throwable {
    override def getMessage: String = s"[Config] Could not find config key $key"
  }
}
