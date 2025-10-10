package com.mikerusoft.examples.compaction

sealed trait Strategy {
  def options: List[OptionsField[?]]
  def where: Option[String]
  def value: String

  def write(): String = {
    val optionsStr = this.options match {
      case List() => ""
      case l: List[OptionsField[?]] => l.map(_.writeValue).mkString(",\n options => map(\n", ",\n", "\n)")
    }
    val whereStr = this.where match {
      case None => ""
      case Some(str) => s",\n where => '$str'"
    }
    s"\n strategy => '$value'$whereStr$optionsStr"
  }
}

case class Binpack (where: Option[String], options: List[OptionsField[?]]) extends Strategy {
  def value: String = "binpack"
}

object Binpack {
  def apply(options: OptionsField[?]*): Binpack = {
    new Binpack(None, options.toList)
  }
  def apply(where: String, options: OptionsField[?]*): Binpack = {
    new Binpack(Strategy.matchWhere(where), options.toList)
  }
}

case class Sort (where: Option[String], sortOrder: Option[String], options: List[OptionsField[?]]) extends Strategy {
  def value: String = "sort"

  override def write(): String = {
    super.write() + (sortOrder match {
      case None => ""
      case Some(str) => s",\n sort_order => '$str'"
    })
  }
}

object Sort {
  def apply(options: OptionsField[?]*): Sort = new Sort(None, None, options.toList)
  def apply(where: String, options: OptionsField[?]*): Sort = new Sort(Strategy.matchWhere(where), None, options.toList)
}

case class Zorder (where: Option[String], sortOrder: List[String], options: List[OptionsField[?]]) extends Strategy {
  def value: String = "sort"
  override def write(): String = {
    super.write() + (sortOrder match {
      case List() => ""
      case l: List[String] => ", \n' zorder(" + l.mkString("", ",", "") + ")'"
    })
  }
}

object Zorder {
  def apply(options: OptionsField[?]*): Zorder = {
    new Zorder(None, List(), options.toList)
  }

  def apply(sortOrder: List[String], options: OptionsField[?]*): Zorder = {
    new Zorder(None, sortOrder, options.toList)
  }

  def apply(where: String, sortOrder: List[String], options: OptionsField[?]*): Zorder = {
    new Zorder(Strategy.matchWhere(where), sortOrder, options.toList)
  }

  def apply(where: String, options: OptionsField[?]*): Zorder = {
    new Zorder(Strategy.matchWhere(where), List(), options.toList)
  }
}

object Strategy {
  def matchWhere(where: Option[String]): Option[String] = {
    where match {
      case None => None
      case Some(str) => if (str.trim.isEmpty) None else Some(str)
    }
  }
  def matchWhere(where: String): Option[String] = {
    if (where.trim.isEmpty) None else Some(where)
  }
}
