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
import fm.common.{InputStreamResource, Normalize, Resource}
import fm.lazyseq.LazySeq
import scala.util.{Failure, Success, Try}
import java.io.{File, InputStream}

/**
 * This is what you extends to implement a new FlatFileReader type
 */
trait FlatFileReaderImpl[IN] extends FlatFileReaderFactory { 
  final def apply(resource: InputStreamResource, options: FlatFileReaderOptions): FlatFileReader = apply(resource.map{ inputStreamToIN(_, options) }, options)

  final def apply(resource: Resource[IN]): FlatFileReader = apply(resource, FlatFileReaderOptions.default)
  final def apply(resource: Resource[IN], options: FlatFileReaderOptions): FlatFileReader = new FlatFileReaderForImpl(resource, options, this)
  
  type LINE
  
  /**
   * Transform an InputStream to whatever our IN type is (IN can be an InputStream in which case this is a no-op)
   */
  def inputStreamToIN(is: InputStream, options: FlatFileReaderOptions): IN
  
  /**
   * Make a line reader given our IN instance
   */
  def makeLineReader(in: IN, options: FlatFileReaderOptions): LazySeq[LINE]
  
  /**
   * Is line considered a blank line?  (for skipping leading blank lines)
   */
  def isBlankLine(line: LINE, options: FlatFileReaderOptions): Boolean
  
  /**
   * Transform from a line reader to a FlatFileParsedRow reader (can be a no-op if LINE is already a FlatFileParsedRow)
   */
  def toParsedRowReader(lineReader: LazySeq[LINE], options: FlatFileReaderOptions): LazySeq[Try[FlatFileParsedRow]]
  
  /**
   * The foreach implementation for an InputStream
   */
  final def foreach[U](is: InputStream, options: FlatFileReaderOptions)(f: Try[FlatFileRow] => U): Unit = foreach(inputStreamToIN(is, options), options)(f)
  
  /**
   * The foreach implementation for the native IN type
   */
  final def foreach[U](in: IN, options: FlatFileReaderOptions)(f: Try[FlatFileRow] => U): Unit = {
    
    val lineReader: LazySeq[LINE] = makeLineReader(in, options).drop(options.skipLines).dropRight(options.skipTrailingLines).dropWhile { line: LINE =>
      isBlankLine(line, options)
    }
    
    var parsedRowReader: LazySeq[Try[FlatFileParsedRow]] = toParsedRowReader(lineReader, options)
    
    if (options.skipEmptyLines) parsedRowReader = parsedRowReader.filterNot{ row: Try[FlatFileParsedRow] => row.isSuccess && (row.get.values.isEmpty || row.get.values.forall{ _.isEmpty }) }
    
    var columnCount: Int = if (options.columnCount > 0) options.columnCount else -1
    
    var headers: FlatFileRowHeaders = if (options.hasHeaders) null else options.headers.map{ FlatFileRowHeaders(_) }.getOrElse{ FlatFileRowHeaders.empty }
    
    import FlatFileReaderOptions.{AutoDetectAllHeaders, AutoDetectAnyHeaders, CustomHeaderDetection, NormalHeaderDetection}
    
    parsedRowReader.foreach {
      case Failure(ex) => f(Failure(ex))  
      case Success(row) =>
        if (null == headers) {          
          // Need to setup headers
          val rowValues: IndexedSeq[String] = options.headerTransform(FlatFileRowHeaders.cleanHeaderValues(row.values))
          val normalizedRowValues: IndexedSeq[String] = rowValues.map{ Normalize.lowerAlphanumeric }
          
          options.headerDetection match {
            // Headers are the first row and should match options.headers (if they exist)
            case NormalHeaderDetection =>
              val headerValues: IndexedSeq[String] = options.headers match {
                case Some(values) => values
                case None => rowValues
              }
              
              headers = makeHeaders(headerValues)
              
              if (!headers.equalsNormalized(rowValues)) throw new FlatFileReaderException.MissingHeaders(s"""FlatFileReaderOptions.headers "${headers.headers.mkString(",")}" does not match read headers: ${rowValues.mkString(",")}""")
              
            case all: AutoDetectAllHeaders => if (all.normalizedHeaders.forall{ normalizedRowValues.contains }) headers = makeHeaders(rowValues) 
            case any: AutoDetectAnyHeaders => if (any.normalizedHeaders.exists{ normalizedRowValues.contains }) headers = makeHeaders(rowValues)
            case custom: CustomHeaderDetection => if (custom.isHeaderRow(rowValues)) headers = makeHeaders(rowValues)
          }

        } else {
          // Headers are set (or don't exist)
          if (-1 == columnCount) {
            if (headers.length > 0) columnCount = headers.length
            else columnCount = row.values.length
          }        
          f(toFlatFileRow(row, columnCount, headers, options))
        }
    }
    
    // If headers aren't set then it's possible that the auto-detection failed
    if (null == headers) options.headerDetection match {
      case NormalHeaderDetection =>
      case all: AutoDetectAllHeaders => throw new FlatFileReaderException.MissingHeaders("Required Headers where not found: "+all.headers.mkString("\"", "\", \"", "\""))
      case any: AutoDetectAnyHeaders => throw new FlatFileReaderException.MissingHeaders("None of the possible Headers where found: "+any.headers.mkString("\"", "\", \"", "\""))
      case custom: CustomHeaderDetection => throw new FlatFileReaderException.MissingHeaders("Headers were not detected")
    }
    
  }
  
  private def makeHeaders(values: IndexedSeq[String]): FlatFileRowHeaders = FlatFileRowHeaders(FlatFileRowHeaders.cleanHeaderValues(values))
  
  private def toFlatFileRow(row: FlatFileParsedRow, columnCount: Int, headers: FlatFileRowHeaders, options: FlatFileReaderOptions): Try[FlatFileRow] = {
    require(columnCount >= 0, "Expected columnCount >= 0")
    require(headers != null, "Expected headers != null")
    
    var values: IndexedSeq[String] = row.values
    
    if(options.enforceColumnCount && values.length != columnCount) {
      if(values.length < columnCount && !options.allowLessColumns || values.length > columnCount) {
        val tmpRow: FlatFileRow = row.toFlatFileRow(headers)
        return Failure(new FlatFileReaderException.ColumnCountMismatch(tmpRow, "Current row column count: "+values.length+", doesn't match expected count: " + columnCount + ", line: '"+row.rawRow+"'\n"+tmpRow.debugMsg()))
      }
    }
    
    if (options.addMissingValues && values.length < columnCount) values = values ++ (values.length until columnCount).map{ i => "" }
    if (options.trimValues) values = values.map{ _.trim() }
    
    Success(FlatFileRow(values, row.rawRow, row.lineNumber, headers))
  }
}