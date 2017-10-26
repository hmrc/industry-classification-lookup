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

import java.nio.file.{FileSystems, Path}

import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.{Directory, NIOFSDirectory}
import sbt.Keys._
import sbt.{TaskKey, _}

object LuceneIndexCreator extends IndexBuilder {

  val indexBuild = TaskKey[Int]("index-build")
  val indexClean = TaskKey[Int]("index-clean")

  val indexSettings = Seq(
    indexBuild := {
      buildIndex()
      0
    },
    indexClean := {
      clean()
      0
    },

    (compile in Compile) <<= (compile in Compile) dependsOn indexBuild
  )
}

trait IndexBuilder {

  val log = ConsoleLogger()
  val path: Path = FileSystems.getDefault().getPath("conf", "index")

  def clean() = {
    FileUtils.deleteDirectory(path.toFile)
  }

  def buildIndex() = {

    val fileSicPipe = "conf/sic-codes.txt"

    clean() // TODO - ideally could check before building on compile

    // TODO clean out the index directory first
    // TODO only build if out of date or missing

    log.info(s"Building new index into ${path.toAbsolutePath}")

    val startTime = System.currentTimeMillis()

    val index: Directory = new NIOFSDirectory(path);

    // TODO doesn't seem to be working - i.e. to drop out IT
    val stopWords = List(
      "a", "an", "and", "are", "as", "at", "be", "but", "by",
      "for", "if", "in", "into", "is", // "it",
      "no", "not", "of", "on", "or", "such",
      "that", "the", "their", "then", "there", "these",
      "they", "this", "to", "was", "will", "with"
    )
    val stopSet = {
      import scala.collection.JavaConverters._
      new CharArraySet(stopWords.asJava, true);
    }

    val analyzer = new StandardAnalyzer(stopSet);
    val config = new IndexWriterConfig(analyzer);
    def addDoc(w: IndexWriter, code: String, description: String) {
      val doc = new Document
      doc.add(new StringField("code8", code, Field.Store.YES))
      doc.add(new TextField("description", description, Field.Store.YES))
      w.addDocument(doc)
    }

    val w = new IndexWriter(index, config);

    import scala.io.Source
    val source = Source.fromFile(fileSicPipe)
    for (line <- source.getLines()) {
      val split = line.split("\\|")
      val code = split(0)
      val desc = split(1)
      addDoc(w, code, desc)
    }
    source.close()

    val numDocs = w.numDocs()
    w.close()

    log.info(s"Index successfully built, ${numDocs} in the index (took ${System.currentTimeMillis - startTime}ms).")
    index
  }
}
