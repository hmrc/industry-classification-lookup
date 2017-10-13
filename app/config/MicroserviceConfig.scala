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

package config

import javax.inject.{Inject, Singleton}

import com.typesafe.config.ConfigValue
import play.api.Configuration

@Singleton
class MicroserviceConfigImpl @Inject()(val playConfig: Configuration) extends MicroserviceConfig

trait MicroserviceConfig {

  protected val playConfig: Configuration

  def getConfigObject(key: String): Set[(String, ConfigValue)] = {
    playConfig.getConfig(key).map(_.entrySet).getOrElse(throw ConfigNotFoundException(key))
  }

  private case class ConfigNotFoundException(key: String) extends Throwable {
    override def getMessage: String = s"Could not find config key $key"
  }
}
