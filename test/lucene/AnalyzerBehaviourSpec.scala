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

import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.{PositionIncrementAttribute, PositionLengthAttribute, TermToBytesRefAttribute}
import uk.gov.hmrc.play.test.UnitSpec


class AnalyzerBehaviourSpec extends UnitSpec {

  trait Setup {
    val analyzer = new StandardAnalyzer()
    def generateTokens(searchString: String) = {
      val tokens = analyzer.tokenStream("foo", searchString)
      tokens.addAttribute(classOf[PositionIncrementAttribute])
      tokens.addAttribute(classOf[PositionLengthAttribute])
      tokens.reset()
      tokens
    }
    def tokensToSeq(tokens: TokenStream): Seq[String] = {
      def next(tokens: TokenStream) = tokens.incrementToken() match {
        case false => None
        case true => Some(tokens.getAttribute(classOf[TermToBytesRefAttribute]).toString)
      }
      next(tokens).fold(Seq[String]()){ Seq(_) ++ tokensToSeq(tokens) }
    }
  }

  "Testing the Analyzer (tweak)" should {

    Seq(
      "foo bar" -> Seq("foo", "bar"),
      "foo the bar" -> Seq("foo", "bar"),
      "foo : bar" -> Seq("foo", "bar"),
      "< > : { } . , | ( )" -> Seq(),
      " " -> Seq(),
      "the" -> Seq()
    ) foreach {
      case ((query, tokens)) => {
        s"return $tokens for search term '$query'" in new Setup {
          val tokens = generateTokens("foo the bar")
          tokensToSeq(tokens) shouldBe Seq("foo", "bar")
        }
      }
    }
  }


  case class Analyzer (
                        tokens: TokenStream,
                        analyzer: StandardAnalyzer,
                        termAtt: TermToBytesRefAttribute
                      )

  def testSetup(searchField: String, searchString: String): Analyzer ={
    val analyzer = new StandardAnalyzer()
    val tokens = analyzer.tokenStream(searchField, searchString)
    val termAtt = tokens.getAttribute(classOf[TermToBytesRefAttribute])
    tokens.addAttribute(classOf[PositionIncrementAttribute])
    tokens.addAttribute(classOf[PositionLengthAttribute])
    tokens.reset()
    Analyzer(tokens, analyzer, termAtt)
  }

  def endTest(tokens: TokenStream): Unit ={
    tokens.end()
    tokens.close()
  }


  "Testing the Analyzers" should {

    "return 2 tokens for a simple string" in {
      val result = testSetup("foo", "foo bar")

      result.tokens incrementToken()
      result.termAtt.toString shouldBe "foo"

      result.tokens.incrementToken()
      result.termAtt.toString shouldBe "bar"

      result.tokens.incrementToken() shouldBe false
      endTest(result.tokens)

    }

    "return 2 tokens for a simple string with a stop word" in {
      val result = testSetup("foo", "foo the bar")

      result.tokens incrementToken()
      result.termAtt.toString shouldBe "foo"

      result.tokens.incrementToken()
      result.termAtt.toString shouldBe "bar"

      result.tokens.incrementToken() shouldBe false
      endTest(result.tokens)
    }

    "return 2 tokens for a simple string with punctuation" in {

      val result = testSetup("foo", "foo : bar")

      result.tokens incrementToken()
      result.termAtt.toString shouldBe "foo"

      result.tokens.incrementToken()
      result.termAtt.toString shouldBe "bar"

      result.tokens.incrementToken() shouldBe false
      endTest(result.tokens)
    }

    "testing punctuation returns nothing" in {
      val result = testSetup("foo", "< > : { } . , | ( )")

      result.tokens.incrementToken() shouldBe false
      endTest(result.tokens)
    }

    "testing search using a space returns no tokens" in {
      val result = testSetup("foo", " ")

      result.tokens.incrementToken() shouldBe false
      endTest(result.tokens)
    }

    "testing search using a stop word only returns no tokens" in {
      val result = testSetup("foo", "the")

      result.tokens.incrementToken() shouldBe false
      endTest(result.tokens)
    }
  }
}
