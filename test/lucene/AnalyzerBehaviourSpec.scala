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

package lucene

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.{PositionIncrementAttribute, PositionLengthAttribute, TermToBytesRefAttribute}
import uk.gov.hmrc.play.test.UnitSpec


class AnalyzerBehaviourSpec extends UnitSpec {

  "Testing the Analyzers" should {

    "return 2 tokens for a simple string" in {
      val analyzer = new StandardAnalyzer()
      val tokens = analyzer.tokenStream("foo", "foo bar")

      val termAtt = tokens.getAttribute(classOf[TermToBytesRefAttribute])
      val posIncAtt = tokens.addAttribute(classOf[PositionIncrementAttribute])
      val posLenAtt = tokens.addAttribute(classOf[PositionLengthAttribute])

      tokens.reset()

      tokens.incrementToken()
      termAtt.toString shouldBe "foo"

      tokens.incrementToken()
      termAtt.toString shouldBe "bar"

      tokens.incrementToken() shouldBe false

      tokens.end()
      tokens.close()
    }

    "return 2 tokens for a simple string with punctuation" in {
      val analyzer = new StandardAnalyzer()
      val tokens = analyzer.tokenStream("foo", "foo : bar")

      val termAtt = tokens.getAttribute(classOf[TermToBytesRefAttribute])
      val posIncAtt = tokens.addAttribute(classOf[PositionIncrementAttribute])
      val posLenAtt = tokens.addAttribute(classOf[PositionLengthAttribute])

      tokens.reset()

      tokens.incrementToken()
      termAtt.toString shouldBe "foo"

      tokens.incrementToken()
      termAtt.toString shouldBe "bar"

      tokens.incrementToken() shouldBe false

      tokens.end()
      tokens.close()
    }

    "testing punctuation returns nothing" in {
      val analyzer = new StandardAnalyzer()
      val tokens = analyzer.tokenStream("foo", "< > : { } . , | ( )")

      val termAtt = tokens.getAttribute(classOf[TermToBytesRefAttribute])
      val posIncAtt = tokens.addAttribute(classOf[PositionIncrementAttribute])
      val posLenAtt = tokens.addAttribute(classOf[PositionLengthAttribute])

      tokens.reset()

      tokens.incrementToken() shouldBe false

      tokens.end()
      tokens.close()
    }
  }
}
