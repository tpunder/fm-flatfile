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

import fm.common.UserFriendlyException

abstract class FlatFileReaderException(msg: String, val friendlyTitle: String, val friendlyMessage: String) extends UserFriendlyException(msg)

object FlatFileReaderException {
  final class ColumnCountMismatch(val row: FlatFileRow, msg: String) extends FlatFileReaderException(msg, "Invalid Column Count Encountered", msg)
  final class MissingHeaders(msg: String) extends FlatFileReaderException(msg, "Unable to Detect Headers", s"""Unable to detect the required headers: $msg""")
  final class DuplicateHeaders(val duplicateHeader: String, msg: String) extends FlatFileReaderException(msg, "Found a Duplicate Header", s"""We found "$duplicateHeader" listed multiple times as a header which means we won't know which column to read the value from for that field.  Please remove the duplicate column and try again.""")
  final class InvalidFlatFile(msg: String) extends FlatFileReaderException(msg, "Invalid or Corrupt File?", """Sorry, but that file either wasn't an Excel/Flat file, or we were unable to parse it.  Please double check the file, if you keep getting this error message please email it to contact@frugalmechanic.com so we can fix our tool.  Thanks!""")
}
