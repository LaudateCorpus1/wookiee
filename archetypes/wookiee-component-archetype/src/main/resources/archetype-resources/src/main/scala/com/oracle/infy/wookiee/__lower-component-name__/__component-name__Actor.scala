/*
 * Copyright (c) 2014. Webtrends (http://www.oracle.infy.com)
 * @author cuthbertm on 11/20/14 12:23 PM
 */
package com.oracle.infy.wookiee.component.${component-name.toLowerCase()}

import akka.actor._
import com.oracle.infy.wookiee.app.HActor
import com.oracle.infy.wookiee.component.StopComponent
import com.oracle.infy.wookiee.health.HealthComponent

import scala.concurrent.Future

object ${component-name}Actor {
  def props = Props(classOf[${component-name}Actor])
}

class ${component-name}Actor extends HActor {

  override def receive = super.receive orElse {
    case StopComponent => // do something to stop the component DELETE if not needed
  }

  // This should probably be overriden to get some custom information about the health of this actor
  override protected def getHealth: Future[HealthComponent] = super.getHealth
}