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
import fm.common.{FileOutputStreamResource, InputStreamResource, Resource, SingleUseResource, XMLUtil}
import fm.lazyseq.LazySeq
import com.frugalmechanic.optparse._
import scala.util.Try
import java.io.{BufferedInputStream, File, Reader}
import org.joda.time.LocalDate

object FlatFileReader extends FlatFileReaderFactory {
  import fm.flatfile.plain.PlainFlatFileReader
  import fm.flatfile.excel.ExcelFlatFileReader

  def apply(resource: InputStreamResource, options: FlatFileReaderOptions): FlatFileReader = new AutoDetectingFlatFileReader(resource, options)
  
  //
  // java.io.Reader is always assumed to be for the PlainFlatFileReader since ExcelFlatFileReader needs an InputStream
  //
  def apply(reader: Reader): FlatFileReader = apply(SingleUseResource(reader))
  def apply(reader: Reader, options: FlatFileReaderOptions): FlatFileReader = apply(SingleUseResource(reader), options)
  
  def apply(reader: Resource[Reader]): FlatFileReader = apply(reader, FlatFileReaderOptions())
  def apply(reader: Resource[Reader], options: FlatFileReaderOptions): FlatFileReader = new FlatFileReaderForImpl[Reader](reader, options, PlainFlatFileReader)
  
  private class AutoDetectingFlatFileReader(resource: InputStreamResource, options: FlatFileReaderOptions) extends FlatFileReader {
    final val withTries: FlatFileReaderWithTries = new AutoDetectingFlatFileReaderWithTries(resource, options)
    def foreach[U](f: FlatFileRow => U): Unit = withTries.map{ _.get }.foreach(f)
  }
  
  private class AutoDetectingFlatFileReaderWithTries(resource: InputStreamResource, options: FlatFileReaderOptions) extends FlatFileReaderWithTries {
    def foreach[U](f: Try[FlatFileRow] => U): Unit = resource.buffered().use { is: BufferedInputStream =>
      val impl: FlatFileReaderImpl[_] = 
        if (ExcelFlatFileReader.isExcelFormat(is)) ExcelFlatFileReader
        else if (XMLUtil.isXML(is)) throw new FlatFileReaderException.InvalidFlatFile("Looks like an XML File and NOT a flat file")
        else PlainFlatFileReader
        
      impl.foreach(is, options)(f)
    }
  }
  
  def parseExcelDate(dateStr: String): LocalDate = ExcelFlatFileReader.parseExcelDate(dateStr)

  //
  // Main Method and Options
  //
  object Options extends OptParse {
    val file = StrOpt()
    val skipLines = IntOpt()
    val debug = BoolOpt()
    val head = IntOpt(desc = "Only take N numbers of rows from the file")
    val out = FileOpt(desc = "Write the outputs out to a file")
    val format = StrOpt(desc = "Valid export formats: 'tsv' or 'csv'", validWith=out)
  }

  def main(args: Array[String]) {
    Options.parse(args)

    val file = new File(Options.file.get)
    val skipLines = Options.skipLines.getOrElse(0)

    require(file.isFile && file.canRead, "File does not exist or is not readable: "+file.getPath)

    val options: FlatFileReaderOptions = new FlatFileReaderOptions(skipLines=skipLines)
    val reader: FlatFileReader = apply(file, options)

    var first = true
    var tmp: LazySeq[FlatFileRow] = reader
    if (Options.head) tmp = tmp.take(Options.head.get)

    optionallyOutput(tmp){ row: FlatFileRow =>
      if (first) {
        println(row.headers.mkString(", "))
        first = false
      }
      if (Options.debug) row.debugPrint() //else println(row.lineNumber+": "+row.values.mkString(", "))
    }
  }

  // Helper for def main(...) for doing file output/conversion
  private def optionallyOutput(reader: LazySeq[FlatFileRow])(f: FlatFileRow => Unit): Unit = {
    if (Options.out) {
      val format: FlatFileWriterOptions = if (Options.format) {
        Options.format() match {
          case "tsv" => FlatFileWriterOptions.TSV
          case "csv" => FlatFileWriterOptions.CSV
          case _ => throw new Exception("Invalid format")
        }
      } else FlatFileWriterOptions.TSV

      println(s"Reading From: ${Options.file()}, Writing to: ${Options.out()}")
      FlatFileWriter(FileOutputStreamResource(Options.out()), options=format) { out: FlatFileWriter =>
        reader.onFirst{ row: FlatFileRow => out.writeRow(row.headers) }.foreach { row: FlatFileRow =>
          out.writeRow(row.values)
          f(row)
        }
      }
    } else {
      reader.foreach{ f }
    }
  }
}

abstract class FlatFileReader extends LazySeq[FlatFileRow] {
  def withTries: FlatFileReaderWithTries
  
  def mapExceptions(f: Throwable => Throwable): LazySeq[FlatFileRow] = {
    withTries.mapExceptions(f).map{ _.get }
  }
}

abstract class FlatFileReaderWithTries extends LazySeq[Try[FlatFileRow]] {
  def mapExceptions(f: Throwable => Throwable): LazySeq[Try[FlatFileRow]] = map{ _.mapFailure(f) }
}
