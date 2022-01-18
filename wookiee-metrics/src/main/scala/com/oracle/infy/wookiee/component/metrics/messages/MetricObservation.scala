/*
 * Copyright 2015 Oracle (http://www.Oracle.com)
 *
 * See the LICENCE.txt file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oracle.infy.wookiee.component.metrics.messages

import com.oracle.infy.wookiee.component.metrics.metrictype._

import java.util.concurrent.TimeUnit

sealed trait MetricObservation extends MetricMessage {}

/**
  * Add or update a specific counter
  * @param metric  the metric definition
  * @param delta the value to adjust by. A positive number will increment, while negative number will decrement
  */
case class CounterObservation(metric: Counter, delta: Int) extends MetricObservation

/**
  * Add or update a specific gauge
  * @param metric  the metric definition
  * @param value the value to update
  */
case class GaugeObservation(metric: Gauge, value: Float) extends MetricObservation

/**
  * Add or update a specific histogram
  * @param metric  the metric definition
  * @param value the value to update
  */
case class HistogramObservation(metric: Histogram, value: Long) extends MetricObservation

/**
  * Add or update a specific meter
  * @param metric  the metric definition
  * @param events the number of events to mark (defaults to 1)
  */
case class MeterObservation(metric: Meter, events: Long = 1) extends MetricObservation

/**
  * Add or update a specific timer
  * @param metric  the metric definition
  * @param elapsed the elapsed time, defaults to nanoseconds
  * @param unit the time unit, defaults to nanoseconds
  */
case class TimerObservation(metric: Timer, elapsed: Long, unit: TimeUnit = TimeUnit.NANOSECONDS)
    extends MetricObservation
