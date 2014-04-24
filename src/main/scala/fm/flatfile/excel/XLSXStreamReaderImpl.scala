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
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.model.StylesTable
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import scala.util.Try
import fm.common.{Logging, SingleUseResource}
import fm.lazyseq.LazySeq
import fm.flatfile.{FlatFileParsedRow, FlatFileReaderOptions}

final class XLSXStreamReaderImpl(is: InputStream, options: FlatFileReaderOptions) extends LazySeq[Try[FlatFileParsedRow]] with Logging {
  def foreach[U](f: Try[FlatFileParsedRow] => U): Unit = {
    val xlsxPackage: OPCPackage = OPCPackage.open(is)
    val stringsTable: ReadOnlySharedStringsTable = new ReadOnlySharedStringsTable(xlsxPackage)
    val xssfReader: XSSFReader = new XSSFReader(xlsxPackage)
    val stylesTable: StylesTable = xssfReader.getStylesTable()

    val sheetsData: java.util.Iterator[InputStream] = xssfReader.getSheetsData
    require(sheetsData.hasNext, "XLSX File Must have at least one sheet")
    
    SingleUseResource(xssfReader.getSheetsData().next).use { sheetInputStream: InputStream =>
      val processor = new XLSXStreamProcessor(options, stylesTable, stringsTable)
      processor.processSheet(sheetInputStream, f)
    }
  }
}