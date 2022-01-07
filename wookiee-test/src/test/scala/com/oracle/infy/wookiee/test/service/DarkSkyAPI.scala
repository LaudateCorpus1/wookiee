package com.oracle.infy.wookiee.test.service

import com.oracle.infy.wookiee.test.TestHarness

import java.net.{HttpURLConnection, URL}
import scala.io.Source._

object DarkSkyAPI {
  private def baseUrl: String = "https://api.darksky.net/forecast/156581be723431440e7c8eda4a41f28c/"

  def getWeather(location: String, connectTimeout: Int = 10000, readTimeout: Int = 5000): String = {
    try {
      val connection = new URL(baseUrl + location).openConnection.asInstanceOf[HttpURLConnection]
      connection.setConnectTimeout(connectTimeout)
      connection.setReadTimeout(readTimeout)
      connection.setRequestMethod("GET")
      val inputStream = connection.getInputStream
      val content = fromInputStream(inputStream).mkString
      if (inputStream != null) inputStream.close()
      content
    } catch {
      case ex: Throwable =>
        TestHarness.log.error(ex, "Failed to get weather")
        s"Failed to get weather: ${ex.getMessage}"
    }
  }
}
