package com.whisk.docker.testkit

import java.io.IOException
import java.net.ServerSocket

object Helpers {

  def newRandomPort(): Int = {
    var server: ServerSocket = null
    try {
      server = new ServerSocket(0)
      server.getLocalPort
    } catch {
      case e: IOException =>
        throw new Error(e)
    } finally if (server != null) try
      server.close()
    catch {
      case ignore: IOException =>
      // ignore
    }
  }
}
