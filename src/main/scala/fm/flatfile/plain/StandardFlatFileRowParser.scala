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

import fm.flatfile.{FlatFileReaderOptions}
import fm.common.{Logging, StacklessException}
import fm.common.Implicits._
import java.util.regex.Pattern
import scala.collection.mutable.Builder

object StandardFlatFileRowParser extends Logging {
  val DefaultQuote: String = "\""
  private object ReparseAsPlainException extends StacklessException

  private[this] val quoteFragment: String = """[\W&&[^ ]]?"""
  private[this] val sepFragment: String = """[\W&&[^ "']]+"""
    
  private[this] val autoDetectTemplate: String = """^(QUOTE).+?\1(SEP)\1.+?\1(\2\1.+?\1)*$"""
  
  def apply(row: CharSequence, options: FlatFileReaderOptions): StandardFlatFileRowParser = {

    val (sep,detectedQuote) = autoDetectSepAndQuote(row, options.comment, options.sep, options.quote)
    
    // The detectedQuote is an Option[String] with values that mean:
    // None - Nothing was auto detected
    // Some("") - We were explicitly told there is no quote
    // Some(quote) - An explicit quote was specified OR we auto-detected a quote
    val quote: String = detectedQuote.getOrElse(StandardFlatFileRowParser.DefaultQuote)
    
    // Strict Quote Escaping means that un-escaped quotes in the middle of a quoted column
    // will trigger us to re-parse the column as an unquoted value.  e.g.:
    // "Foo" Bar"
    // With strictQuoteEscaping enabled will parse as:  "Foo" Bar"
    // And with strictQuoteEscaping disabled will parse as: Foo" Bar
    //
    // This allows us to correctly parse stuff like:
    // "Super Cool" Product
    // Which looks like it's a quoted column but really isn't.
    val strictQuoteEscaping: Boolean = detectedQuote.isDefined
    
    val parser: StandardFlatFileRowParser = new StandardFlatFileRowParser(sep=sep, quote=quote, comment=options.comment, strictQuoteEscaping=strictQuoteEscaping)

    logger.info("Using sep: "+parser.sep.replace("\t","\\t")+"  quote: "+parser.quote)

    parser
  }
    
  def autoDetectPattern(sep: String, quote: FlatFileReaderOptions.QuoteOption): Pattern = {
    val sepPattern: String = if(null != sep && "" != sep) Pattern.quote(sep) else sepFragment
    val quotePattern: String = quote match {
      case FlatFileReaderOptions.AutoDetectQuote  => quoteFragment
      case FlatFileReaderOptions.NoQuote          => ""
      case FlatFileReaderOptions.ExplicitQuote(q) => Pattern.quote(q)+"?"
    }
    
    autoDetectTemplate.replace("SEP", sepPattern).replace("QUOTE", quotePattern).r.pattern
  }

  def autoDetectSepAndQuote(row: String, comment: String): (String,Option[String]) = autoDetectSepAndQuote(row, comment, null, FlatFileReaderOptions.AutoDetectQuote)
  
  def autoDetectSepAndQuote(row: CharSequence, comment: String, sep: String, quote: FlatFileReaderOptions.QuoteOption): (String,Option[String]) = {
    
    // Check if there is anything to auto detect
    if (null != sep && "" != sep) quote match {
      case FlatFileReaderOptions.AutoDetectQuote  => 
      case FlatFileReaderOptions.NoQuote          => return (sep, Some(""))
      case FlatFileReaderOptions.ExplicitQuote(q) => return (sep, Some(q))
    }

    val matcher = autoDetectPattern(sep, quote).matcher(row)

    if (!matcher.matches) {
      throw new Exception("Unable to auto detect sep and quote for line: "+row)
    }

    val matchedQuote: String = matcher.group(1).trim()
    
    val detectedQuote: Option[String] = quote match {
      case FlatFileReaderOptions.AutoDetectQuote  => matchedQuote.toBlankOption // Default to the quote char if nothing was detected 
      case FlatFileReaderOptions.NoQuote          => Some("")
      case FlatFileReaderOptions.ExplicitQuote(q) => 
        require(q == matchedQuote, "Expected explicit quote and detected quote to match!  Explicit: '"+q+"'  Detected: '"+matchedQuote+"'")
        Some(q)
    }

    val detectedSep: String = matcher.group(2)
    // Can't use .isNotNullOrBlank because a tab counts as being blank (all whitespace)
    if (null != sep && "" != sep) require(sep == detectedSep, "Expected explicit seperator and detected seperator to match!  Explicit: '"+sep+"'  Detected: '"+detectedSep+"'")

    require(!detectedSep.startsWith(" ") && !detectedSep.endsWith(" "), "Sep starts or ends with spaces: \""+detectedSep+"\"")
    require(!detectedQuote.exists{ _ == detectedSep }, "Sep and Quote are the same?!: '"+detectedSep+"'")

    (detectedSep,detectedQuote)
  }

  private case class ResumeInfo(columnValues: Builder[String, Vector[String]], idx: Int, tmpIdx: Int, columnValueBuffer: StringBuilder)

}

final class StandardFlatFileRowParser(val sep: String, val quote: String, val comment: String = null, strictQuoteEscaping: Boolean = false) extends FlatFileRowParser {
  import StandardFlatFileRowParser.{ReparseAsPlainException, ResumeInfo}

  private[this] val escapedQuote: String = quote+quote
  
  // Hack to add \" support for escaping quotes (used by mysqldump)
  // TODO: build out better support for this or make it an option
  private[this] val slashEscapedQuote: String = "\\"+quote

  final def parseRow(row: CharSequence): IndexedSeq[String] = {
    if (0 == row.length) return Vector.empty

    // If this is a comment line return an empty record
    if (comment.isNotNullOrBlank && row.nextCharsMatch(comment, 0)) return Vector.empty
    
    parseRowImpl(row)
  }
  
  final def resumeParsingRow(row: CharSequence, obj: AnyRef): IndexedSeq[String] = {
    val data: ResumeInfo = obj.asInstanceOf[ResumeInfo]
    parseRowImpl(row, data.columnValues, data.columnValueBuffer, data.idx, data.tmpIdx, true)
  }

  /**
   * row - The row we are parsing
   * columnValues - This is what will be returned
   * columnValueBuffer - Temp reusable buffer for building up the individual column values
   * _idx - The saved idx value (if we are resuming)
   * _tmpIdx - The saved negative tmpIdx value (if we are resuming)
   * isResume - Are we resuming parsing a column?
   */
  private def parseRowImpl(
      row: CharSequence,
      columnValues: Builder[String, Vector[String]] = Vector.newBuilder,
      columnValueBuffer: StringBuilder = new StringBuilder,
      _idx: Int = 0,
      _tmpIdx: Int = 0,
      isResume: Boolean = false
  ): IndexedSeq[String] = {

    var idx: Int = _idx
    
    //
    // Special Logic for Resuming of Parsing when we need another line of data
    //
    if (isResume) {
      var tmpIdx: Int = _tmpIdx
      
      val newIdx: Int = try {
        // Continue parsing where we left off (removing the negative from tmpIdx)
        tmpIdx = parseRestOfQuotedColumnValue(row, -1*tmpIdx, columnValueBuffer)
        
        // Negative value means we need another line of data
        if (tmpIdx < 0) {
          throw NeedAnotherLineException(ResumeInfo(columnValues, idx, tmpIdx, columnValueBuffer))
        }
        
        tmpIdx
      } catch {
        case ReparseAsPlainException => 
          columnValueBuffer.clear()
          parsePlainColumnValue(row, idx, columnValueBuffer)
      }
      
      columnValues += columnValueBuffer.toString
      columnValueBuffer.clear()
      idx = newIdx
    }
    
    //
    // Normal Parsing Logic
    //
    while (idx < row.length) {
      if (Character.isWhitespace(row.charAt(idx)) && !row.nextCharsMatch(sep, idx) && !row.nextCharsMatch(quote, idx)) {
        // Skip over any whitespace (unless it's our sep!!!) but add it to the buffer in case we are parsing a non-quoted
        // column since in that case we want the space
        columnValueBuffer += row.charAt(idx)
        idx += 1
      } else {
        val newIdx: Int = if(row.nextCharsMatch(quote, idx)) {
          // Since we are parsing a quoted value we need to clear out any whitespace
          // that was added to the buffer
          columnValueBuffer.clear()

          try {
            // Parse with quotes
            val tmpIdx: Int = parseQuotedColumnValue(row, idx, columnValueBuffer)
            
            // Negative value means we need another line of data
            if (tmpIdx < 0) {
              throw NeedAnotherLineException(ResumeInfo(columnValues, idx, tmpIdx, columnValueBuffer))
            }
            
            tmpIdx
          } catch {
            case ReparseAsPlainException => 
              columnValueBuffer.clear()
              parsePlainColumnValue(row, idx, columnValueBuffer)
          }
        } else {
          // Parse without quotes (until the next sep), return newIdx
          parsePlainColumnValue(row, idx, columnValueBuffer)
        }

        columnValues += columnValueBuffer.toString
        columnValueBuffer.clear()
        idx = newIdx
      }
    }

    if (row.nextCharsMatch(sep, row.length-sep.length)) columnValues += "" // Add a blank record if the last character is a separator
    columnValues.result()
  }


  /**
   * Parse a quoted value for a column
   * @param readBuffer The source buffer to read data from
   * @param idx The index to start at
   * @param result Where to store the resulting column value
   * @return The index we left off on
   */
  final def parseQuotedColumnValue(readBuffer: CharSequence, idx: Int, result: StringBuilder): Int = {
    assert(readBuffer.nextCharsMatch(quote, idx))
    assert(idx >= 0, s"Negative idx: $idx")
    // Increment past the quote and parse until the end of the column
    parseRestOfQuotedColumnValue(readBuffer, idx+quote.length, result)
  }

  final def parseRestOfQuotedColumnValue(readBuffer: CharSequence, idx: Int, result: StringBuilder): Int = {
    assert(idx >= 0, s"Negative idx: $idx")
    
    var i: Int = idx

    while (i < readBuffer.length) {
      if(readBuffer.nextCharsMatch(escapedQuote, i)) {
        // We found an escaped quote, add 1 copy of it
        result ++= quote
        i += escapedQuote.length
      /* START HACK */
      /* START HACK */
      /* START HACK */
      } else if (readBuffer.nextCharsMatch(slashEscapedQuote, i)) {
        // We found an escaped quote, add 1 copy of it
        result ++= quote
        i += slashEscapedQuote.length
      /* END HACK */
      /* END HACK */
      /* END HACK */
      } else if (readBuffer.nextCharsMatch(quote, i)) {
        // This is either the end of the column or a non-escaped quote in the middle of the column

        // If this is the end of the column then we would expect the quote to be followed
        // by optional whitespace and either a sep or the end of the buffer
        var tmpI: Int = i + quote.length

        // Skip over any whitespace after the quote (ignoring our sep/quote since they could be considered whitespace: e.g. \t)
        while (tmpI < readBuffer.length && Character.isWhitespace(readBuffer.charAt(tmpI)) && !readBuffer.nextCharsMatch(quote, tmpI) && !readBuffer.nextCharsMatch(sep, tmpI)) {
          tmpI += 1
        }

        // If we are past the end of the buffer or we found a sep then that's the end of the field
        if (tmpI >= readBuffer.length || readBuffer.nextCharsMatch(sep, tmpI)) {
          return tmpI + sep.length
        } else {
          // Otherwise we found an improperly escaped quote in the middle of the column.  If we are using
          // strictQuoteEscaping then reparse this column as a plain un-quoted column
          if (strictQuoteEscaping) throw ReparseAsPlainException
          result ++= quote
          i += quote.length
        }
      } else {
        result += readBuffer.charAt(i)
        i += 1
      }
    }

    // If we ran out of buffer then we should return our negative position to signal that we need another line
    -1 * i
    }

  /**
   * Parse a non-quoted value for a column
   * @param readBuffer The source buffer to read data from
   * @param idx The index to start at
   * @param result Where to store the resulting column value
   * @return The index we left off on
   */
  final def parsePlainColumnValue(readBuffer: CharSequence, idx: Int, result: StringBuilder): Int = {
    assert(idx >= 0, s"Negative idx: $idx")
    
    var i: Int = idx
    while (i < readBuffer.length) {
      if (readBuffer.nextCharsMatch(sep, i)) return i+sep.length
      result += readBuffer.charAt(i)
      i += 1
    }

    // If we get here then we reached the end of the buffer
    i
  }

  override def toString = "StandardFlatFileRowParser(sep='"+sep+"', quote='"+quote+"')"
}