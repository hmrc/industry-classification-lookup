
object ONSSicBuilder extends SICIndexBuilder {

  val name = "ons"

  def produceDocuments(addDocument: ONSSicBuilder.AddDocument) = {
    import scala.io.Source
    var docsAdded = 0
    val fileSicPipe = "index-src/ONSSupplementDataSet.txt"
    val source = Source.fromFile(fileSicPipe)
    for (line <- source.getLines()) {
      val split = line.split("\t")
      val code = split(0)
      val desc = split(1)
      addDocument(SicDocument(code, desc, desc))
      docsAdded += 1
    }
    source.close()

    docsAdded
  }
}
