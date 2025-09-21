package com.mikerusoft.examples.tools

object Profiles {
  def findProfile(profile: Option[String] = None): String = {
    profile match {
      case None =>
        var profile = System.getenv("PROFILE")
        if (profile == null || "".equals(profile))
          profile = System.getProperty("PROFILE")
        profile
      case Some(p) => p
    }
  }
}
