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
package fm.flatfile.excel

import fm.flatfile.{FlatFileParsedRow, FlatFileReaderImpl, FlatFileReaderOptions}
import fm.common.Implicits._
import fm.common.{ImmutableArray, InputStreamResource}
import fm.lazyseq.LazySeq
import java.io.{BufferedInputStream, File, InputStream}
import java.time.{LocalDate, YearMonth}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.util.Try
import org.apache.poi.poifs.filesystem.FileMagic

object ExcelFlatFileReader extends ExcelFlatFileReader {
  // java.lang.String doesn't trim "no break space" - 160
  def nbspTrim(s: String): String = {
    s.replace(160.toChar, ' ').trim
  }
  
  def isExcelFormat(f: File): Boolean = InputStreamResource.forFileOrResource(f).buffered().use{ isExcelFormat }
  def isXLSFormat(f: File): Boolean = InputStreamResource.forFileOrResource(f).buffered().use{ isXLSFormat }
  def isXLSXFormat(f: File): Boolean = InputStreamResource.forFileOrResource(f).buffered().use{ isXLSXFormat }
  
  def isExcelFormat(is: BufferedInputStream): Boolean = isXLSFormat(is) || isXLSXFormat(is)
  def isXLSFormat(is: BufferedInputStream): Boolean = FileMagic.valueOf(is) === FileMagic.OLE2
  def isXLSXFormat(is: BufferedInputStream): Boolean = FileMagic.valueOf(is) === FileMagic.OOXML
  
  def makeLineReader(in: InputStream, options: FlatFileReaderOptions): LazySeq[Try[FlatFileParsedRow]] = {
    val bis: BufferedInputStream = in.toBufferedInputStream

    if (isXLSFormat(bis)) new XLSStreamReaderImpl(bis, options)
    else if (isXLSXFormat(bis)) new XLSXStreamReaderImpl(bis, options)
    else throw ExcelFlatFileReaderException.InvalidExcelFile("Unable to Detect Excel File Type")
  }
  
  private val excelLocalDateParsers: ImmutableArray[DateTimeFormatter] = {
    ImmutableArray(
        "M/d/yyyy",           // 3/14/2001 => 1/1/2013
        "M-d-yyyy",           // 3-14-2001 => 1-1-2013
        "E, MMM d, yyyy",      // Wed, Mar 14, 2001 => Tue, Jan 1, 2013
        "E, MMMM d, yyyy",   // Wed, March 14, 2001 => Tue, January 1, 2013
        "EEEE, MMM d, yyyy",    // Wednesday, Mar 14, 2001 => Tuesday, Jan 1, 2013
        "EEEE, MMMM d, yyyy", // Wednesday, March 14, 2001 => Tuesday, January 1, 2013
        // 3/14 => Skipping no year
        "M/d/yy",             // 3/14/01 => 1/1/13
        "M-d-yy",             // 3-14-01 => 1-1-13
        "MM/dd/yy",           // 03/14/01 => 01/01/13
        // 14-Mar => Skipping no year
        "d-MMM-yy",           // 14-Mar-01 => 1-Jan-13
        "dd-MMM-yy",          // 14-Mar-01 => 01-Jan-13
        "MMMM d, yyyy",       // March 14, 2011 => January 1, 2013
        "M/d/yy h:m a",       // 3/14/01 1:30 PM => 1/1/13 12:00 AM
        "M/d/yy k:m",         // 3/14/01 13:30 PM => 1/1/13 0:00
        "M/d/yy k:m",         // 3/14/01 13:30 PM => 1/1/13 0:00
        // M => Skipping no year
        // M-01 => Skipping March or May is same
        "d-MMM-yyyy",         // 14-Mar-2001 => 1-Jan-2013
        
        // Other Common Formats:
        "yyyy-MM-dd",         // 2013-01-01
        "yyyy-M-d",           // 2013-1-1
        "yyyy/MM/dd",         // 2013/01/01
        "yyyy/M/d"            // 2013/1/1
    ).map{ DateTimeFormatter.ofPattern(_) }
  }

  private val excelYearMonthParsers: ImmutableArray[DateTimeFormatter] = {
    ImmutableArray(
      "MMM-yy",   // Mar-01 => Jan-13
      "MMMM-yy"   // March-01 => January-13
    ).map{ DateTimeFormatter.ofPattern(_) }
  }
  
  /**
   * Attempts to parse an excel formatted date
   */
  def parseExcelDate(dateStr: String): LocalDate = {
    require(dateStr.isNotNullOrBlank, "dateStr is blank!")

    {
      var i: Int = 0

      while (i < excelLocalDateParsers.length) {
        try {
          return fixupLocalDateYear(LocalDate.parse(dateStr, excelLocalDateParsers(i)))
        } catch {
          case _: DateTimeParseException => // Ignore the exception and try the next format
        }

        i += 1
      }
    }

    {
      var i: Int = 0

      while (i < excelYearMonthParsers.length) {
        try {
          val yearMonth: YearMonth = YearMonth.parse(dateStr, excelYearMonthParsers(i))
          return fixupLocalDateYear(yearMonth.atDay(1))
        } catch {
          case _: DateTimeParseException => // Ignore the exception and try the next format
        }

        i += 1
      }
    }

    // If we get to this point then parsing failed
    throw new ExcelFlatFileReaderException.InvalidDateFormat(s"Invalid Date Format encountered: $dateStr")
  }

  private def fixupLocalDateYear(date: LocalDate): LocalDate = {
    // If this was a 2-digit year auto-correct it to be the proper 20XX year
    if (date.getYear < 100) date.withYear(2000 + date.getYear) else date
  }
  
  def tryParseExcelDate(dateStr: String): Option[LocalDate] = Try{ parseExcelDate(dateStr) }.toOption
  
  /**
   * Takes the 1-based column index and returns the excel column letter
   */
  def excelColumnLettersForIdx(idx: Int): String = {
    require(idx > 0, s"Invalid index: $idx")
    
    var column: String = ""
    
    var num: Int = idx
    
    while (num > 0) {
      val ch: Int = (num - 1) % 26
      column = (ch + 65).toChar + column
      num = (num - ch + 1) / 26
    }
    
    column
  }
}

/**
 * The base trait for the various Excel flat file reader implementations
 */
trait ExcelFlatFileReader extends FlatFileReaderImpl[InputStream] {

  type LINE = Try[FlatFileParsedRow]

  /** To Be Implemented by child class */
  def makeLineReader(in: InputStream, options: FlatFileReaderOptions): LazySeq[Try[FlatFileParsedRow]]

  /** no-op */
  final def inputStreamToIN(is: InputStream, options: FlatFileReaderOptions): InputStream = is
  
  final def isBlankLine(line: Try[FlatFileParsedRow], options: FlatFileReaderOptions): Boolean = {
    // TODO: figure out if we want to trigger the exception here via the Try.get.  Would there even be exceptions for reading Excel lines?
    line.get.values.forall{ _.isNullOrBlank }
  }
  
  /** no-op */
  final def toParsedRowReader(lineReader: LazySeq[Try[FlatFileParsedRow]], options: FlatFileReaderOptions): LazySeq[Try[FlatFileParsedRow]] = lineReader
}