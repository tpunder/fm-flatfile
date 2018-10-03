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
import fm.common.OutputStreamResource
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

object FlatFileWriter {
  def apply[T](out: OutputStreamResource)(f: FlatFileWriter => T): T = {
    apply(out, FlatFileWriterOptions.default)(f)
  }

  def apply[T](out: OutputStreamResource, options: FlatFileWriterOptions)(f: FlatFileWriter => T): T = {
    apply(out, UTF_8, options)(f)
  }

  def apply[T](out: OutputStreamResource, charset: Charset)(f: FlatFileWriter => T): T = {
    apply(out, UTF_8, FlatFileWriterOptions.default)(f)
  }

  def apply[T](out: OutputStreamResource, charset: Charset, options: FlatFileWriterOptions)(f: FlatFileWriter => T): T = {
    out.writer(charset).map{ new FlatFileWriter(_, options) }.use{ flatFileWriter: FlatFileWriter =>
      flatFileWriter.writeHeaders()
      f(flatFileWriter)
    }
  }
}

final class FlatFileWriter(writer: Writer, options: FlatFileWriterOptions) {
  private[this] val hasQuote: Boolean = options.quote.isNotNullOrBlank
  private[this] val quotedQuote: String = if (hasQuote) options.quote+options.quote else ""

  private[this] var headerLookup: FlatFileRowHeaders = _
  private[this] var headerSize: Int = _
  private[this] var headersWritten: Boolean = false
  private[this] var firstRecord: Boolean = true
  
  private def setHeaders(headers: IndexedSeq[String]): Unit = {
    headerLookup = FlatFileRowHeaders(headers)
    headerSize = headers.size
  }
  
  setHeaders(options.headers.getOrElse{ Vector.empty })
    
  def writeHeaders(): Unit = {
    require(!headersWritten, "Headers have already been written!")
    
    if (options.writeHeaders && options.headers.isDefined) {
      writeRow(options.headers.get)
      headersWritten = true
    }
  }
  
  def write(kvPairs: (String, String)*): Unit = {
    // Use the headers from the kv/pairs
    if (!headersWritten && options.writeHeaders) {
      val headers: IndexedSeq[String] = kvPairs.map{ _._1 }.toIndexedSeq
      
      options.headers match {
        case Some(h) => require(h == headers, s"Expected options.headers $h to match detected headers $headers")
        case None => setHeaders(headers)
      }
      
      writeRow(headers)
      headersWritten = true
    }
    
    val arr: Array[String] = new Array(headerSize)
    
    kvPairs.foreach { case (key, value) =>
      val idx: Option[Int] = headerLookup.getColIndexForKey(key)
      require(idx.isDefined, s"Missing header index for header key: $key  Headers: ${options.headers}")
      arr(idx.get) = value
    }
    
    writeRow(arr)
  }
  
  def writeRow(row: Seq[String]): Unit = {
    if (firstRecord) {
      firstRecord = false
    } else {
      if (!options.trailingNewline) writer.write(options.newline)
    }
    
    var firstColumn: Boolean = true
    
    row.foreach { s: String =>
      if (!firstColumn) writer.write(options.sep) else firstColumn = false
      writer.write(quote(s))
    }
    
    if (options.trailingNewline) writer.write(options.newline)
  }
  
  private def quote(_s: String): String = {
    var s: String = _s
    
    if (null == s) s = ""
    
    if (hasQuote) {
      // Handle escaping of quotes
      if (s.contains(options.quote)) s = s.replace(options.quote, quotedQuote)
      
      // Handle quoting if there is a sep/quote/newline
      if (s.contains(options.sep) || s.contains(options.quote) || s.contains("\n")) s = options.quote+s+options.quote
    } else {
      ???
    }
    
    s
  }
}