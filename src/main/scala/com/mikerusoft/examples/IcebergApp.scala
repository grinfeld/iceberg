package com.mikerusoft.examples

import com.mikerusoft.examples.tools.Profiles

object IcebergApp {
  def main(args: Array[String]): Unit = {
    val profile = Profiles.findProfile()
    SimpleIcebergApp.runApp(profile)
  }
}