package com.example

object IcebergApp {
  def main(args: Array[String]): Unit = {
    val profile = System.getProperty("PROFILE")
    SimpleIcebergApp.runApp(profile)
  }
}