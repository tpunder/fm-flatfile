/*
 * Copyright 2014 Frugal Mechanic (http://frugalmechanic.com)
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
package fm.flatfile

import fm.common.Implicits._
import java.math.BigDecimal
import java.io.{PrintWriter, StringWriter}

/**
 * The parsed representation of a Row with methods for reading the columns based on optional headers
 */

final case class FlatFileRow(values: IndexedSeq[String], rawRowCharSequence: CharSequence, lineNumber: Long, rowHeaders: FlatFileRowHeaders) {
  require(null != rawRowCharSequence, "rawRow cannot be null")
  
  final def rawRow: String = rawRowCharSequence.toString

  /**
   * Column header names
   */
  final def headers: IndexedSeq[String] = rowHeaders.headers

  /**
   * Access the value of a row by it's index
   */
  final def apply(idx: Int): String = values(idx)
  final def get(idx: Int): Option[String] = if(idx < values.length) values(idx).toBlankOption else None

  /**
   * Access the value of a row by it's column name
   */
  final def apply(col: String): String = apply(rowHeaders.colIndexForKey(col))
  final def get(col: String): Option[String] = rowHeaders.getColIndexForKey(col).flatMap{ get }

  /**
   * Check if a value for a column exists
   */
  final def hasCol(col: String): Boolean = rowHeaders.hasColIndexForKey(col) && hasCol(rowHeaders.colIndexForKey(col))
  final def hasCol(idx: Int): Boolean = idx < values.length
  
  /**
   * Converts this row to a Seq of Name -> Value
   */
  final def toTuples: IndexedSeq[(String, String)] = (headers zip values)
  
  /**
   * Converts this row to a Map of Name -> Value
   */
  final def toMap: Map[String, String] = toTuples.toUniqueMap

  /**
   * The number of column values
   */
  final def size = values.length

  final def debugPrint(): Unit = print(debugMsg())
    
  final def debugMsg(): String = {
    val sw: StringWriter = new StringWriter()
    val pw: PrintWriter = new PrintWriter(sw)
    
    pw.println()
    pw.println("=====================================================================================")
    pw.println("Header Size: "+headers.length)
    pw.println("Values Size: "+values.length)
    pw.println("Line Number: "+lineNumber)
    pw.println("=====================================================================================")
    pw.println(rawRow)
    pw.println("=====================================================================================")
    
    val maxHeaderWidth: Int = headers.map{_.length}.sorted.lastOption.getOrElse(0)
    val maxIndexWidth: Int = math.max(headers.length, values.length).toString.length
    
    headers.zipWithIndex.foreach{ case (h, i) =>
      val v = if(hasCol(h)) this(h) else "<null>"
      pw.println("("+i.toString.lPad(maxIndexWidth, ' ')+") "+h.rPad(maxHeaderWidth, ' ')+" => "+v)
    }
    // Show any extra values
    (headers.length until values.length).foreach{ i =>
      val pad = "".rPad(maxHeaderWidth, ' ')
      pw.println("("+i.toString.lPad(maxIndexWidth, ' ')+") "+pad+" => "+values(i))
    }
    pw.println("=====================================================================================")
    
    pw.flush()
    pw.close()
    sw.toString()
  }
  
  /**
   * Like getNonBlank except returns null instead of None if the column has no value
   */
  final def getNonBlankOrNull(fields: String*): String = {
    var i = 0
    
    while (i < fields.length) {
      val field: String = fields(i)
      if (hasCol(field)) {
        val v: String = apply(field)
        if (v.isNotBlank) return v
      }
      i += 1
    }
    
    null
  }
  
  // Original:
  //def getNonBlank(fields: String*): Option[String] = fields.findMapped { get(_).filter(_.isNotBlank) }
  
  // This is faster than the original
  final def getNonBlank(fields: String*): Option[String] = Option(getNonBlankOrNull(fields: _*))

  final def getInt(fields: String*): Option[Int] = getBigDecimal(fields: _*).map{_.intValue()}

  final def getBigDecimal(fields: String*): Option[BigDecimal] = {
    getNonBlank(fields: _*).filter{_.isBigDecimal}.map{_.toBigDecimal}
  }

  final override def toString = if(null != values) values.toString else "null"
}