/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.utils

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.core.read.ListAppender
import config.ICLConfig

import scala.jdk.CollectionConverters._

class IndexConnectorSpec extends PlaySpec with MockitoSugar {

  class TestConnector(val config: ICLConfig) extends IndexConnector {
    override val name: String = "testIndex"
  }

  implicit val request = FakeRequest()

  val testConfig = Configuration("index.path" -> "/tmp/index")
  val mockServicesConfig = mock[ServicesConfig]

  val realICLConfig = new ICLConfig(mockServicesConfig, testConfig)

  "analyzer" should {

    "return StandardAnalyzer with English stop words for 'en' language" in {
      val connector = new TestConnector(realICLConfig)
      val analyzer = connector.analyzer("en")
      analyzer mustBe a [org.apache.lucene.analysis.standard.StandardAnalyzer]
    }

    "return StandardAnalyzer with English stop words for 'en-GB' language" in {
      val connector = new TestConnector(realICLConfig)
      val analyzer = connector.analyzer("en-GB")
      analyzer mustBe a [org.apache.lucene.analysis.standard.StandardAnalyzer]
    }

    "return StandardAnalyzer with Welsh stop words for 'cy' language" in {
      val connector = new TestConnector(realICLConfig)
      val analyzer = connector.analyzer("cy")
      analyzer mustBe a [org.apache.lucene.analysis.standard.StandardAnalyzer]
    }

    "return StandardAnalyzer with Welsh stop words for 'cy-GB' language" in {
      val connector = new TestConnector(realICLConfig)
      val analyzer = connector.analyzer("cy-GB")
      analyzer mustBe a [org.apache.lucene.analysis.standard.StandardAnalyzer]
    }

    "log a warning and fallback to English stop words for unsupported language" in {
      val connector = new TestConnector(realICLConfig)

      val logger = LoggerFactory.getLogger(connector.getClass).asInstanceOf[Logger]
      val listAppender = new ListAppender[ch.qos.logback.classic.spi.ILoggingEvent]()
      listAppender.start()
      logger.addAppender(listAppender)

      val unsupportedLang = "xx"
      val analyzer = connector.analyzer(unsupportedLang)
      analyzer mustBe a [org.apache.lucene.analysis.standard.StandardAnalyzer]

      val logs = listAppender.list.asScala
      logs.exists(event =>
        event.getLevel == Level.WARN &&
          event.getFormattedMessage.contains("falling back to English (en).")
      ) mustBe true

      logger.detachAppender(listAppender)
      listAppender.stop()
    }
  }
}
