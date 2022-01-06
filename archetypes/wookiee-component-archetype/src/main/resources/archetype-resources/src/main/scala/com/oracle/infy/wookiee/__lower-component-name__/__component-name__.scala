/*
 * Copyright (c) 2014. Webtrends (http://www.oracle.infy.com)
 * @author cuthbertm on 11/20/14 12:23 PM
 */
package com.oracle.infy.wookiee.component.${component-name.toLowerCase()}

import akka.actor.ActorRef
import com.oracle.infy.wookiee.component.Component

trait ${component-name} { this: Component =>

  var ${component-name}Ref:Option[ActorRef] = None

  def start${component-name} : ActorRef = {
    ${component-name}Ref = Some(context.actorOf(${component-name}Actor.props, ${component-name}.${component-name}Name))
    ${component-name}Ref.get
  }

  def stop${component-name} = {
    //TODO execute any special logic here to shut down the component
  }
}

object ${component-name} {
  val ${component-name}Name = "${component-name}"
}