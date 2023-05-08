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

import java.io.InputStream
import java.lang.Double
import org.apache.poi.ss.usermodel.{BuiltinFormats, DataFormatter, RichTextString}
import org.apache.poi.xssf.model.StylesTable
import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRichTextString}
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import scala.util.Try
import fm.flatfile.{FlatFileParsedRow, FlatFileReaderOptions}
import fm.common.Logging

private[excel] final class XLSXStreamProcessor(options: FlatFileReaderOptions, stylesTable: StylesTable, stringsTable: ReadOnlySharedStringsTable) extends Logging {
  private implicit def toRichXMLStreamReader2(sr: org.codehaus.stax2.XMLStreamReader2): RichXMLStreamReader2 = new RichXMLStreamReader2(sr)
  
  private[this] val formatter: DataFormatter = new DataFormatter()
  
  // Created a cached copy of the plain shared strings from the shared strings table
  private[this] val plainSharedStrings: Array[String] = new Array(stringsTable.getUniqueCount)
  (0 until stringsTable.getUniqueCount).foreach { (i: Int) =>
    plainSharedStrings(i) = richStringToPlain(stringsTable.getItemAt(i))
  }

  private def richStringToPlain(s: String): String = richStringToPlain(new XSSFRichTextString(s))
  private def richStringToPlain(rich: RichTextString): String = rich.toString
  
  def processSheet[U](sheetInputStream: InputStream, f: Try[FlatFileParsedRow] => U): Unit = {
    import com.ctc.wstx.stax.WstxInputFactory
    import org.codehaus.stax2.XMLStreamReader2
    import javax.xml.stream.XMLInputFactory
    
    val inputFactory: WstxInputFactory = new WstxInputFactory()
    inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
    inputFactory.configureForSpeed()

    val sr: XMLStreamReader2 = inputFactory.createXMLStreamReader(sheetInputStream).asInstanceOf[XMLStreamReader2]
    
    // Not using Resource.using to avoid Proguard complaining about XMLStreamReader2
    try {
      sr.seekToRootElement("worksheet")
      
      // foreach row in the sheetData
      sr.foreach("sheetData/row") { 
        val row: Try[FlatFileParsedRow] = Try {
          val rowNumber: Long = sr.getAttributeValue("r").toLong // This is the row number
          
          val values = Vector.newBuilder[String]
          var lastColIdx: Int = -1
          
          // foreach cell in the row
          sr.foreach("c") {
            val ref: String = sr.getAttributeValue("r")
            val colIdx: Int = referenceToColumn(ref)
            val cellType: String = sr.getAttributeValue("t")
            val cellStyle: String = sr.getAttributeValue("s")
            
            // Fill is missing columns
            if (lastColIdx + 1 != colIdx) {
              var i: Int = lastColIdx + 1
              while (i < colIdx) {
                values += ""
                i += 1
              }
            }
            
            lastColIdx = colIdx
            
            var value: String = ""
            
            if (cellType == "inlineStr") sr.foreach("is/t"){ value = richStringToPlain(sr.readElementText()) }
            else value = sr.tryReadChildElementText("v").map{ (value: String) => formatCell(cellType, cellStyle, value) }.getOrElse("")
            
            values += value
          }
          
          FlatFileParsedRow(values = values.result(), rawRow = "", lineNumber = rowNumber)
        }
        
        f(row)
      }
    } finally {
      sr.closeCompletely()
    }
  }
  
  /**
   * cellType and cellStyle might be null
   */
  private def formatCell(cellType: String, cellStyle: String, value: String): String = cellType match {
    case "b"         => if (value.charAt(0) == '0') "FALSE" else "TRUE" // Boolean
    case "e"         => value // Error -- TODO: should this just return an empty string instead of the #DIV/0! stuff?
    case "inlineStr" => throw new AssertionError("This should not happen!")
    case "s"         => sharedStringTableValue(value) // Shared Strings Table Index
    case "str"       => value // A Formula Calculated String
    case _ => {
      // It's a number, but almost certainly one with a special style or format
      if (null != cellStyle) {
        val styleIndex: Int = cellStyle.toInt
        val style: XSSFCellStyle = stylesTable.getStyleAt(styleIndex)
        val formatIndex: Short = style.getDataFormat()
        var formatString: String = style.getDataFormatString()
        if (formatString == null) formatString = BuiltinFormats.getBuiltinFormat(formatIndex)
        formatter.formatRawCellContents(Double.parseDouble(value), formatIndex, formatString)
      } else value
    }
  }
  
  def sharedStringTableValue(idx: String): String = try {
    plainSharedStrings(idx.toInt)
  } catch {
    case ex: NumberFormatException => logger.warn(s"Failed to parse SST index '$idx': ${ex.toString()}"); ""
    case ex: ArrayIndexOutOfBoundsException => logger.warn(s"ArrayIndexOutOfBoundsException for SST index '$idx': ${ex.toString()}"); ""
  }
  
  /**
   * Converts an Excel reference name like "C123" to the column zero-based index
   */
  final def referenceToColumn(name: String): Int = {
    var i = 0
    while (i < name.length && name.charAt(i).isLetter) {
      i += 1
    }
    nameToColumn(name.substring(0, i))
  }
  
  /**
   * Converts an Excel column name like "C" to a zero-based index.
   */
  final def nameToColumn(name: String): Int = {
    var column: Int = -1
    var i = 0
    
    while (i < name.length) {
      val c: Int = name.charAt(i)
      column = (column + 1) * 26 + c - 'A'
      i += 1
    }

    column
  }
}
