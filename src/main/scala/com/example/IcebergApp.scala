package com.example

object IcebergApp {
  def main(args: Array[String]): Unit = {
    val profile = findProfile()
    SimpleIcebergApp.runApp(profile)
  }

  private def findProfile(): String = {
    var profile = System.getenv("PROFILE")
    if (profile == null || "".equals(profile))
      profile = System.getProperty("PROFILE")

    profile
  }
}