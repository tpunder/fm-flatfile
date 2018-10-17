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
package fm.flatfile.plain

import fm.flatfile.{FlatFileParsedRow, FlatFileReaderImpl, FlatFileReaderOptions}
import fm.common.{InputStreamResource, Resource}
import fm.common.Implicits._
import fm.lazyseq.LazySeq
import scala.util.Try
import java.io.Reader

object PlainFlatFileReader extends FlatFileReaderImpl[Reader] {
  type LINE = LineWithNumber

  def inputStreamResourceToResourceIN(resource: InputStreamResource, options: FlatFileReaderOptions): Resource[Reader] = {
    resource.readerWithDetectedCharset()
  }
  
  def makeLineReader(reader: Reader, options: FlatFileReaderOptions): LazySeq[LINE] = {
    val tmp: LazySeq[LINE] = new LineReader(reader).zipWithIndex.map{ case (line, idx) => LineWithNumber(line, idx + 1) }
    options.plainLineReaderTransform(tmp)
  }
  
  def isBlankLine(lineWithNumber: LineWithNumber, options: FlatFileReaderOptions): Boolean = {
    val line: CharSequence = lineWithNumber.line
    // Skip leading empty rows or commented rows
    line.isNullOrBlank || (options.comment.isNotNullOrBlank && line.nextCharsMatch(options.comment, 0))
  }
  
  def toParsedRowReader(lineReader: LazySeq[LineWithNumber], options: FlatFileReaderOptions): LazySeq[Try[FlatFileParsedRow]] = {
    new PlainParsedRowReader(lineReader, options)
  }
}