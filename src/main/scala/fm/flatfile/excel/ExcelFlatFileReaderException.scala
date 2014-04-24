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

import fm.flatfile.FlatFileReaderException

sealed abstract class ExcelFlatFileReaderException(msg: String, title: String, message: String) extends FlatFileReaderException(msg, title, message)

object ExcelFlatFileReaderException {
  final case class InvalidExcelFile(msg: String) extends ExcelFlatFileReaderException(msg, "Invalid or Corrupt Excel File?", """Sorry, but that file either wasn't an Excel file, or we were unable to parse it.  Please double check the file, if you keep getting this error message please email it to contact@frugalmechanic.com so we can fix our tool.  Thanks!""")
  final case class InvalidDateFormat(msg: String) extends ExcelFlatFileReaderException(msg, "Unknown Date Format Detected.", s"""Sorry, but it appears we encountered an unknown date format in your file.  Please see this error message for details: $msg.  Please double check the file, if you think this is a valid date format please email it to contact@frugalmechanic.com so we can fix our tool.  Thanks!""")
  final case class EmptyFile(msg: String) extends ExcelFlatFileReaderException(msg, "File Uploaded Was Empty.", """It appears the file you tried uploading is empty. Please double check the file, if you think this is a valid date format please email it to contact@frugalmechanic.com so we can fix our tool.  Thanks!""")
}
