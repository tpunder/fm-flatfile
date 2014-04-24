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

import fm.flatfile.FlatFileReader

object TestExcelFlatFileReader {
  val headers: IndexedSeq[String] = Vector("text_header", "formatted_header", "boolean_header", "date_header", "time_header", "datetime_header", "number_header", "double_header", "formula_header", "error_header", "sharedstrings_header", "accountingformat_no_symbol_header", "accountingformat_symbol_header")

  val values = IndexedSeq(
    Vector("text_value1", "formatted_value1", "TRUE", "8/7/12", "5:01:01 AM", "8/7/12 5:01", "1", "1.10", "11.1whee", "#DIV/0!", "sharedstrings_value", "4.79", "$4.79"),
    Vector("", "formatted_value2", "FALSE", "8/8/12", "5:02:02 AM", "8/8/12 5:02", "2", "2.20", "22.2whee", "#DIV/0!", "sharedstrings_value", "5.79", "$5.79"),
    Vector("text_value3", "formatted_value3", "TRUE", "8/9/12", "5:03:03 AM", "8/9/12 5:03", "3", "3.30", "33.3whee", "#DIV/0!", "", "6.79", "$6.79"),
    Vector("text_value4", "formatted_value4", "FALSE", "8/10/12", "5:04:04 AM", "8/10/12 5:04", "4", "4.40", "44.4whee", "#DIV/0!", "sharedstrings_value", "7.79", "$7.79"),
    Vector("text_value5", "formatted_value5", "TRUE", "", "5:05:05 AM", "8/11/12 5:05", "5", "5.50", "55.5whee", "#DIV/0!", "sharedstrings_value", "8.79", "$8.79"),
    Vector("text_value6", "formatted_value6", "FALSE", "8/12/12", "5:06:06 AM", "8/12/12 5:06", "6", "6.60", "66.6whee", "#DIV/0!", "sharedstrings_value", "9.79", "$9.79")
  )
  
  // These are the line numbers that correspond to the values
  val lineNumbers = IndexedSeq(2, 3, 4, 5, 6, 8)
}

// These should auto-detect the type of Excel file
final class TestExcelFlatFileReaderXLS extends TestExcelFlatFileReaderCommon(ExcelFlatFileReader, "XLSStreamReaderTestWorkbook.xls")
final class TestExcelFlatFileReaderXLSX extends TestExcelFlatFileReaderCommon(ExcelFlatFileReader, "XLSXStreamReaderTestWorkbook.xlsx")

final class TestFlatFileReaderXLS extends TestExcelFlatFileReaderCommon(FlatFileReader, "XLSStreamReaderTestWorkbook.xls")
final class TestFlatFileReaderXLSX extends TestExcelFlatFileReaderCommon(FlatFileReader, "XLSXStreamReaderTestWorkbook.xlsx")

final class TestXLSFlatFileReader extends TestExcelFlatFileReaderCommon(XLSFlatFileReader, "XLSStreamReaderTestWorkbook.xls")
final class TestXLSXFlatFileReader extends TestExcelFlatFileReaderCommon(XLSXFlatFileReader, "XLSXStreamReaderTestWorkbook.xlsx")