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

import org.scalatest.FunSuite
import org.scalatest.Matchers
import fm.common.TestHelpers

final class TestExcelFlatFileReaderDateParsing extends FunSuite with Matchers {
  
  test("Excel Date Parsing") {
    check("8/1/11")
    check("08/01/11")
    
    check("8/1/2011")
    check("08/01/2011")
    
    check("8-1-2011")
    check("08-01-2011")
    
    check("Monday, August 1, 2011")
    
    check("1-Aug-11")
    check("01-Aug-11")
    
    check("Aug-11")
    check("August-11")
    
    check("August 1, 2011")
    check("August 01, 2011")
    
    check("8/1/11 1:30 PM")
    check("8/01/11 1:30 PM")
    
    check("8/1/11 1:30")
    check("8/01/11 1:30")
    
    check("1-Aug-2011")
    check("01-Aug-2011")
    
    check("2011-8-1")
    check("2011-08-01")
  }
  
  def check(s: String, expected: String = "2011-08-01"): Unit = TestHelpers.withCallerInfo {
    ExcelFlatFileReader.parseExcelDate(s).toString should equal (expected)
  }
}