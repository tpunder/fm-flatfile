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

import fm.flatfile.{FlatFileParsedRow, FlatFileReaderOptions}
import fm.common.Resource
import fm.lazyseq.LazySeq
import java.io.InputStream
import scala.util.Try

final class XLSStreamReader(resource: Resource[InputStream], options: FlatFileReaderOptions) extends LazySeq[Try[FlatFileParsedRow]] {
  def foreach[U](f: Try[FlatFileParsedRow] => U): Unit = resource.use { is =>
    val reader = new XLSStreamReaderImpl(is, options)
    reader.foreach(f)
  }
}
