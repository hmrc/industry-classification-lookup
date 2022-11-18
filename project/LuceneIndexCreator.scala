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

import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.packager.MappingsHelper.contentOf
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.{Analyzer, CharArraySet}
import org.apache.lucene.document._
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.{Directory, NIOFSDirectory}
import sbt.Keys._
import sbt.{Append, Compile, ConsoleLogger, Def, File, TaskKey, fileToRichFile}

import scala.io.Source

object LuceneIndexCreator {

  val log = ConsoleLogger()
  val indexBuild = TaskKey[Seq[File]]("index-build")

  import Append._

  val indexBuildTask = Def.task {
    val root = (Compile / resourceManaged).value / "conf" / "index"

    val builders: Seq[SICIndexBuilder] = Seq(GDSSicBuilder, ONSSicBuilder)

    val files = builders.map {
      // Perform the actual index build
      _.buildIndex(root)
    } flatMap { location =>
      // convert the location into a Sequence of files to be packaged
      Seq(location) ++ location.listFiles()
    }

    log.debug(s"Index files to be copied by resource generator ${files.mkString(",")}")
    files
  }

  val indexSettings = Seq(
    indexBuild := indexBuildTask.value,
    Compile / resourceGenerators += indexBuildTask,
    Universal / mappings ++= contentOf((Compile / resourceManaged).value),
    // clean the old location where indexes were stored
    cleanFiles += baseDirectory { base => base / "conf"/ "index" }.value
  )
}

case class SicDocument(
                        code: String,
                        description: String,
                        searchTerms: String,
                        descriptionCy: String,
                        searchTermsCy: String
                      )

trait SICIndexBuilder extends IndustryCodeMapping with StopWords {

  val LANG_EN = "en"
  val LANG_CY = "cy"
  val FIELD_SEARCH_TERMS = "searchTerms"
  val FIELD_SEARCH_TERMS_CY = "searchTermsCy"

  // Override this with the name of the index
  val name: String

  def getIndexSource: Source

  // Implement this method, calling the passed in addDocument Function for each new Indexed document required
  def produceDocuments(addDocument: ONSSicBuilder.AddDocument): Int = {
    var docsAdded = 0
    val source = getIndexSource
    for (line <- source.getLines()) {
      val split = line.split("\t")
      val code = split(0)
      val desc = split(1)
      val descCy = split(2)
      addDocument(SicDocument(code, desc, desc, descCy, descCy))
      docsAdded += 1
    }
    source.close()

    docsAdded
  }

  type AddDocument = SicDocument => Boolean

  val log = ConsoleLogger()

  def buildIndex(rootPath: File): File = {
    val sic8Path = rootPath / name
    val indexSic8Path = sic8Path.toPath

    val index: Directory = new NIOFSDirectory(indexSic8Path)

    // Only build if missing
    if( index.listAll().length == 0 ) {

      log.info(s"""Building new index "$name" into ${indexSic8Path.toAbsolutePath}""")

      val startTime = System.currentTimeMillis()

      val analyzer = {
        import scala.collection.JavaConverters._
        val analyzer: String => Analyzer = (lang: String) => {
          val stopWords = lang match {
            case LANG_EN => STOP_WORDS.asJava
            case LANG_CY => STOP_WORDS_CY.asJava
          }
          new StandardAnalyzer(new CharArraySet(stopWords, true))
        }

        new PerFieldAnalyzerWrapper(
          new StandardAnalyzer(),
          Map(FIELD_SEARCH_TERMS -> analyzer(LANG_EN), FIELD_SEARCH_TERMS_CY -> analyzer(LANG_CY)).asJava
        )
      }

      val config = new IndexWriterConfig(analyzer)
      val facetConfig = new FacetsConfig()

      val w = new IndexWriter(index, config)

      val numAdded = produceDocuments(addDoc(w, facetConfig))

      val numIndexDocs = w.numDocs()

      w.commit() // flush the contents
      w.close()

      log.info(s"""Index "$name" successfully built, $numIndexDocs in the index (adding $numAdded took ${System.currentTimeMillis - startTime}ms).""")
    }

    sic8Path
  }

  def addDoc(w: IndexWriter, facetConfig: FacetsConfig)(sicDoc: SicDocument): Boolean = {
    val doc = new Document
    doc.add(new StringField("code", sicDoc.code, Field.Store.YES))
    doc.add(new StoredField("description", sicDoc.description))
    doc.add(new StoredField("descriptionCy", sicDoc.descriptionCy))
    doc.add(new TextField("searchTerms", sicDoc.searchTerms, Field.Store.NO))
    doc.add(new TextField("searchTermsCy", sicDoc.searchTermsCy, Field.Store.NO))
    doc.add(new SortedSetDocValuesFacetField("sector", returnIndustrySector(sicDoc.code)))
    w.addDocument(facetConfig.build(doc))
    true
  }


}

trait StopWords {
  val STOP_WORDS: List[String] = List(
    "a", "an", "and", "are", "as", "at", "be", "but", "by",
    "for", "if", "in", "into", "is", // "it",
    "no", "not", "of", "on", "or", "such",
    "that", "the", "their", "then", "there", "these",
    "they", "this", "to", "was", "will", "with"
  )

  val STOP_WORDS_CY: List[String] = List()
}

trait IndustryCodeMapping {
  private val industryCodeMapping = Map.apply(
    "01" -> "A", "02" -> "A", "03" -> "A",
    "05" -> "B", "06" -> "B", "07" -> "B", "08" -> "B", "09" -> "B",
    "10" -> "C", "11" -> "C", "12" -> "C", "13" -> "C", "14" -> "C", "15" -> "C", "16" -> "C", "17" -> "C",
    "18" -> "C", "19" -> "C", "20" -> "C", "21" -> "C", "22" -> "C", "23" -> "C", "24" -> "C", "25" -> "C",
    "26" -> "C", "27" -> "C", "28" -> "C", "29" -> "C", "30" -> "C", "31" -> "C", "32" -> "C", "33" -> "C",
    "35" -> "D",
    "36" -> "E", "37" -> "E", "38" -> "E", "39" -> "E",
    "41" -> "F", "42" -> "F", "43" -> "F",
    "45" -> "G", "46" -> "G", "47" -> "G",
    "49" -> "H", "50" -> "H", "51" -> "H", "52" -> "H", "53" -> "H",
    "55" -> "I", "56" -> "I",
    "58" -> "J", "59" -> "J", "60" -> "J", "61" -> "J", "62" -> "J", "63" -> "J",
    "64" -> "K", "65" -> "K", "66" -> "K",
    "68" -> "L",
    "69" -> "M", "70" -> "M", "71" -> "M", "72" -> "M", "73" -> "M", "74" -> "M", "75" -> "M",
    "77" -> "N", "78" -> "N", "79" -> "N", "80" -> "N", "81" -> "N", "82" -> "N",
    "84" -> "O",
    "85" -> "P",
    "86" -> "Q", "87" -> "Q", "88" -> "Q",
    "90" -> "R", "91" -> "R", "92" -> "R", "93" -> "R",
    "94" -> "S", "95" -> "S", "96" -> "S",
    "97" -> "T", "98" -> "T",
    "99" -> "U"
  )

  def returnIndustrySector(sicCode: String): String = {
    val firstTwoChars = sicCode.substring(0,2)
    industryCodeMapping.getOrElse(firstTwoChars,
      throw new Exception(s"Industry code for sic-code $sicCode does not exist")
    )
  }
}
