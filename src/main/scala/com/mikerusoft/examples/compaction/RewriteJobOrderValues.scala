package com.mikerusoft.examples.compaction

sealed trait RewriteJobOrderValues {
  def value: String
}
object RewriteJobOrderValues {
  def BytesAsc: RewriteJobOrderValues = BytesAscRewriteJobOrder()
  def BytesDesc: RewriteJobOrderValues = BytesDescRewriteJobOrder()
  def FilesAsc: RewriteJobOrderValues = FilesAscRewriteJobOrder()
  def FilesDesc: RewriteJobOrderValues = FilesDescRewriteJobOrder()
  def None: RewriteJobOrderValues = NoneRewriteJobOrder()

  case class BytesAscRewriteJobOrder private () extends RewriteJobOrderValues {
    override def value: String = "bytes-asc"
  }
  case class BytesDescRewriteJobOrder private () extends RewriteJobOrderValues {
    override def value: String = "bytes-desc"
  }
  case class FilesAscRewriteJobOrder private () extends RewriteJobOrderValues {
    override def value: String = "files-asc"
  }
  case class FilesDescRewriteJobOrder private () extends RewriteJobOrderValues {
    override def value: String = "files-desc"
  }
  case class NoneRewriteJobOrder private () extends RewriteJobOrderValues {
    override def value: String = "none"
  }
}