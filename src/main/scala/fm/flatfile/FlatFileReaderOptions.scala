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
import fm.common.Normalize
import fm.lazyseq.LazySeq
import fm.flatfile.plain.LineWithNumber

object FlatFileReaderOptions {
  sealed trait HeaderDetection
  
  /** Use normal header detection logic based on the other FlatFileReaderOptions */
  case object NormalHeaderDetection extends HeaderDetection
  
  /** ALL of these headers must exist to auto-detect the header row */
  final case class AutoDetectAllHeaders(headers: Seq[String]) extends HeaderDetection {
    val normalizedHeaders: IndexedSeq[String] = headers.map{ Normalize.lowerAlphanumeric }.toIndexedSeq
  }
  
  /** ANY of these headers can exist to auto-detect the header row */
  final case class AutoDetectAnyHeaders(headers: Seq[String]) extends HeaderDetection {
    val normalizedHeaders: IndexedSeq[String] = headers.map{ Normalize.lowerAlphanumeric }.toIndexedSeq
  }
  
  /** 
   * Use a custom function to detect headers
   * 
   * The first row that the isHeaderRow function returns true for will be used as the headers
   * 
   * NOTE: The raw (un-normalized) values are passed into the isHeaderRow function
   */
  final case class CustomHeaderDetection(isHeaderRow: IndexedSeq[String] => Boolean) extends HeaderDetection
  
  sealed trait QuoteOption
  case object NoQuote extends QuoteOption
  case object AutoDetectQuote extends QuoteOption
  final case class ExplicitQuote(quote: String) extends QuoteOption
  
  implicit def toQuoteOption(quote: String): QuoteOption = if(quote.isNullOrBlank) NoQuote else ExplicitQuote(quote)
  implicit def toQuoteOption(quote: Option[String]): QuoteOption = toQuoteOption(quote.getOrElse(null))
  
  val default: FlatFileReaderOptions = FlatFileReaderOptions()
}

final case class FlatFileReaderOptions(
  //
  // Commons Options:
  //
  skipLines: Int = 0,
  skipTrailingLines: Int = 0,
  skipEmptyLines: Boolean = true,
  hasHeaders: Boolean = true,
  headers: Option[IndexedSeq[String]] = None,
  enforceColumnCount: Boolean = false,
  allowLessColumns: Boolean = false, // If enforceColumnCount == true, allow less than the expected number of columns
  columnCount: Int = -1,
  addMissingValues: Boolean = true, // Add empty values for any missing columns
  trimValues: Boolean = true,
  dumpRowDetailsOnException: Boolean = true,
  headerDetection: FlatFileReaderOptions.HeaderDetection = FlatFileReaderOptions.NormalHeaderDetection,
  // Allows you to apply a custom transform to the header row.  This transform is applied after FlatFileRowHeaders.cleanHeaderValues is applied (so you have trimmed values and trailing empty values removed)
  headerTransform: IndexedSeq[String] => IndexedSeq[String] = headers => headers,
  //
  // PlainFlatFileReader Specific Options:
  //
  sep: String = null,
  quote: FlatFileReaderOptions.QuoteOption = FlatFileReaderOptions.AutoDetectQuote,
  characterEncoding: String = null,
  comment: String = null,  // Lines at the beginning of the file that begin with this string will be treated as a comment
  plainLineReaderTransform: LazySeq[LineWithNumber] => LazySeq[LineWithNumber] = reader => reader,  // Allows you to apply arbitrary transforms to the Line Reader.  This is run BEFORE anything else is applied (e.g. skip lines)
  //
  // ExcelFlatFileReader Specific Options:
  //
  sheetName: String = null
) {
  if (!hasHeaders) require(headerDetection == FlatFileReaderOptions.NormalHeaderDetection, "If hasHeaders is false then headerDetection must be NormalHeaderDetection since no other options make sense!")
}