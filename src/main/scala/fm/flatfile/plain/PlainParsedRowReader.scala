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

import fm.flatfile.{FlatFileParsedRow, FlatFileReaderOptions}
import fm.lazyseq.LazySeq
import java.lang.{StringBuilder => JavaStringBuilder}

import scala.util.{Failure, Success, Try}

/**
 * Reads FlatFileParsedRows from a LineReader
 */
final class PlainParsedRowReader(lineReader: LazySeq[LineWithNumber], options: FlatFileReaderOptions) extends LazySeq[Try[FlatFileParsedRow]] {  
  
  private case class ResumeInfo(lineNumber: Long, data: AnyRef)
  
  def foreach[U](f: Try[FlatFileParsedRow] => U): Unit = {    
    var parser: FlatFileRowParser = null
    var row: JavaStringBuilder = null
    
    // For resuming parsing when we need another line
    var resumeInfo: ResumeInfo = null
    
    def reset(): Unit = {
      resumeInfo = null
      row = null
    }
    
    lineReader.foreach { lineWithNumber: LineWithNumber =>      
      val nextLine: JavaStringBuilder = lineWithNumber.line 
      
      // If the line isn't null then we append the nextLine because this must be a multi-line row
      if (null == row) row = nextLine else {
        row.append("\n")
        row.append(nextLine)
      }
      
      if (null == parser) {
        // Need to do auto detections / setup parser...
        parser = StandardFlatFileRowParser(row, options)
      }
      
      def lineNumber: Long = if (null == resumeInfo) lineWithNumber.num else resumeInfo.lineNumber
      
      try {
        val columnValues: IndexedSeq[String] = if (resumeInfo == null) parser.parseRow(row) else parser.resumeParsingRow(row, resumeInfo.data)
        f(Success(FlatFileParsedRow(columnValues, row, lineNumber)))
        reset()
      } catch {
        case NeedAnotherLineException(data) => resumeInfo = ResumeInfo(lineNumber, data)
        case ex: Exception =>
          reset()
          f(Failure(ex))
      }     
      
    }
    
  }
  
}