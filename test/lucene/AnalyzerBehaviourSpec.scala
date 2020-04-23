/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatestplus.play.PlaySpec

class AnalyzerBehaviourSpec extends PlaySpec {

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

      next(tokens).fold(Seq[String]()) {
        Seq(_) ++ tokensToSeq(tokens)
      }
    }
  }

  "Testing the Analyzer" should {

    Seq(
      "foo bar" -> Seq("foo", "bar"),
      "foo the bar" -> Seq("foo", "bar"),
      "foo : bar" -> Seq("foo", "bar"),
      "< > : { } . , | ( )" -> Seq(),
      " " -> Seq(),
      "the" -> Seq()
    ) foreach {
      case (query, tokens) => {
        s"return $tokens for search term '$query'" in new Setup {
          val tok = generateTokens(query)
          tokensToSeq(tok) mustBe tokens
          tok.close()
        }
      }
    }
  }

}
