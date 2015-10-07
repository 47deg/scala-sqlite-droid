/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2012 47 Degrees, LLC http://47deg.com hello@47deg.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package com.fortysevendeg.mvessel.util

import java.sql.ResultSet

import com.fortysevendeg.mvessel.util.StructureControlProcessor._

import scala.util.Try

object ResultSetProcessorOps {

  implicit def `ResultSet processor` = new StructureControlProcessor[ResultSet] {

    def move(resultSet: ResultSet): Boolean = resultSet.next()

    def close(resultSet: ResultSet): Unit = Try(resultSet.close())

    def isClosed(resultSet: ResultSet): Boolean = resultSet.isClosed

  }

  implicit class ResultSetOps(resultSet: ResultSet) {

    def process[T](process: ResultSet => T, until: Option[Int] = None) = processStructureControl(resultSet)(process, until)

    def processOne[T](process: ResultSet => T) = processStructureControl(resultSet)(process, Some(1)).headOption

  }

}