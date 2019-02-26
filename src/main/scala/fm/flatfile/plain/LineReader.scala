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

import fm.lazyseq.LazySeq
import java.io.Reader
import java.lang.{StringBuilder => JavaStringBuilder}

final class LineReader(reader: Reader) extends LazySeq[JavaStringBuilder] {
  private[this] val BufferSize: Int = 1024
  
  def foreach[U](f: JavaStringBuilder => U): Unit = {
    var sb: JavaStringBuilder = new JavaStringBuilder
    
    val buf: Array[Char] = new Array(BufferSize)
    var bufSize: Int = -1
        
    do {
      bufSize = reader.read(buf)
      
      var idx: Int = 0
      
      while(idx < bufSize) {
        val ch: Char = buf(idx)
        
        if (ch == '\n') {
          f(sb)
          sb = new JavaStringBuilder
        } else if (isValidChar(ch)) {
          sb.append(ch)
        }
        
        idx += 1
      }
      
    } while (bufSize != -1)
      
    // If the last line is empty, skip it
    if (sb.length > 0) f(sb)
  }
  
  private def isValidChar(ch: Char): Boolean = !ignoreChar(ch)
  
  private def ignoreChar(ch: Char): Boolean = {
    '\t' != ch && Character.isISOControl(ch)
  }
}