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

import fm.common.InputStreamResource
import fm.flatfile.{FlatFileReaderFactory, FlatFileReaderOptions, FlatFileRow}
import java.io.{BufferedInputStream, File}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

abstract class TestExcelFlatFileReaderCommon[IN](factory: FlatFileReaderFactory, file: String) extends AnyFunSuite with Matchers {

  private def isXLS: Boolean = file.toLowerCase.endsWith(".xls")
  private def isXLSX: Boolean = file.toLowerCase.endsWith(".xlsx")
  
  // Sanity check that the file should match one of the types
  require(isXLS ^ isXLSX)
  
  private def flatFileRows(options: FlatFileReaderOptions = FlatFileReaderOptions()): IndexedSeq[FlatFileRow] = {
    factory(new File(file), options).toIndexedSeq
  }
  
  test("File Type Detection") {
    InputStreamResource.forResource(new File(file)).buffered().use{ (is: BufferedInputStream) =>
      // These are called multiple times to make sure the underlying implementation is 
      // properly using mark()/reset() on the BufferedInputStream
      ExcelFlatFileReader.isXLSFormat(is) should equal (isXLS)
      ExcelFlatFileReader.isXLSXFormat(is) should equal (isXLSX)
      ExcelFlatFileReader.isXLSFormat(is) should equal (isXLS)
      ExcelFlatFileReader.isXLSXFormat(is) should equal (isXLSX)
    }
  }
  
  test("Headers") {
    flatFileRows().foreach{ (row: FlatFileRow) =>
      row.headers should equal (TestExcelFlatFileReader.headers)
    }
  }

  test("Values") {
    flatFileRows().map{ r => r.values}.toIndexedSeq should equal(TestExcelFlatFileReader.values)
  }

  test("Line Numbers") {
    flatFileRows().zip(TestExcelFlatFileReader.lineNumbers).foreach { case (row, lineNumber) =>
      row.lineNumber should equal (lineNumber)
    }
  }
  
  test("Header Options - 'test_data' sheetName") {
    val options = FlatFileReaderOptions(sheetName = "test_data")
    flatFileRows(options).map{ _.values }.toIndexedSeq should equal (TestExcelFlatFileReader.values)
  }
  
  test("Header Options - 'should_not_extract' sheetName with no headers") {
    val options = FlatFileReaderOptions(hasHeaders = false, sheetName = "SHOULD_not_ExTrAcT")
    flatFileRows(options).map{ _.values }.toIndexedSeq should equal (TestExcelFlatFileReader.shouldNotExtractValues)
  }
  
  test("Header Options - 'sheet3' sheetName with no headers") {
    val options = FlatFileReaderOptions(hasHeaders = false, sheetName = "shEet3")
    flatFileRows(options).map{ _.values }.toIndexedSeq should equal (Vector.empty)
  }

  test("Header Options - Specify Row") {
    val headers: IndexedSeq[String] = TestExcelFlatFileReader.values(1) // 0 Based + 1 for Header
    val options = FlatFileReaderOptions(skipLines = 2)
    flatFileRows(options).foreach{ (row: FlatFileRow) => row.headers should equal (headers) }
  }

  test("Header Options - AutoDetect Row - ALL") {
    val headers: IndexedSeq[String] = TestExcelFlatFileReader.values(3) // 0 Based + 1 for Header
    val detectHeaders = headers.slice(0,2)
    val options = FlatFileReaderOptions(headerDetection = FlatFileReaderOptions.AutoDetectAllHeaders(detectHeaders))
    flatFileRows(options).forall{ _.headers == headers } should equal(true)
  }
  
  test("Header Options - AutoDetect Row - ANY") {
    val headers: IndexedSeq[String] = TestExcelFlatFileReader.values(3) // 0 Based + 1 for Header
    val detectHeaders = headers.slice(0,2)
    val options = FlatFileReaderOptions(headerDetection = FlatFileReaderOptions.AutoDetectAnyHeaders(detectHeaders))
    flatFileRows(options).forall{ _.headers == headers } should equal(true)
  }
}