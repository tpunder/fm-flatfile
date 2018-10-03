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

import java.util.concurrent.ConcurrentHashMap
//import fm.fastutil.FastAny2IntHashMap
import scala.collection.mutable.{HashMap => MutableHashMap}
import fm.common.Implicits._
import fm.common.Normalize

object FlatFileRowHeaders {
  val empty: FlatFileRowHeaders = FlatFileRowHeaders(Vector.empty)
  
  def cleanHeaderValues(values: IndexedSeq[String]): IndexedSeq[String] = {
    // Drop trailing blank values
    values.map{ _.trim }.reverse.dropWhile{ _.isNullOrBlank }.reverse
  }
}

final case class FlatFileRowHeaders(headers: IndexedSeq[String]) {
  require(headers != null, "headers is null?!")
  
  // Caching seems to give a decent speedup for the AWDAFlatFileToXmlConverter
  private val useCache: Boolean = true
  
  // This will cache the raw values
  private val columnNameToIndexCache = new ConcurrentHashMap[String, Integer]
  
  final def length: Int = headers.length
  
  /**
   * Map of Header Name to Value Idx
   * 
   * If the index is set to -1 then it means there is a duplicate header
   */
  private val headerMap: MutableHashMap[String, Int] = {
    val m = new MutableHashMap[String, Int]
    headers.zipWithIndex.foreach { case (name, idx) =>
      if (m.contains(name)) m(name) = -1
      else m(name) = idx 
    }
    m
  }
  
//  private val headerMap: FastAny2IntHashMap[String] = {
//    val m = new FastAny2IntHashMap[String]
//    headers.zipWithIndex.foreach { case (name, idx) =>
//      if (m.contains(name)) m(name) = -1
//      else m(name) = idx 
//    }
//    m
//  }
  
  /**
   * Map of Normalized Header Name to Value Idx
   * 
   * If the index is set to -1 then it means there is a duplicate header
   */
   private val normalHeaderMap: MutableHashMap[String, Int] = {
    val m = new MutableHashMap[String, Int]
    headers.zipWithIndex.foreach { case (name, idx) =>
      val normalName = Normalize.lowerAlphanumeric(name)
      if (m.contains(normalName)) m(normalName) = -1
      else m(normalName) = idx 
    }
    m
  }
  
//  private val normalHeaderMap: FastAny2IntHashMap[String] = {
//    val m = new FastAny2IntHashMap[String]
//    headers.zipWithIndex.foreach { case (name, idx) =>
//      val normalName = Normalize.lowerAlphanumeric(name)
//      if (m.contains(normalName)) m(normalName) = -1
//      else m(normalName) = idx 
//    }
//    m
//  }

  /**
   * Is there a column for this key?
   * 
   * Note: duplicate columns (-1, FlatFileReaderException.DuplicateHeaders) return false
   */
  final def hasColIndexForKey(key: String): Boolean = cachedRawColIndexForKey(key) >= 0
  
  /**
   * Get the column index for a key. Possible returns are:
   * >= 0         - The index of the column for this key
   * -1           - This key is a duplicate
   * Int.MinValue - No such key
   */
  private def rawColIndexForKey(key: String): Int = {
    if (headerMap.contains(key)) headerMap(key)
    else  {
      val normalizedKey: String = Normalize.lowerAlphanumeric(key)
      if (normalHeaderMap.contains(normalizedKey)) normalHeaderMap(normalizedKey)
      else Int.MinValue
    }
  }
  
  private def cachedRawColIndexForKey(key: String): Int = {
    if (useCache) {
      val cachedIdx: Integer = columnNameToIndexCache.get(key)
      if (null != cachedIdx) return cachedIdx.intValue()
    }
    
    val idx: Int = rawColIndexForKey(key)
    
    if (useCache) columnNameToIndexCache.putIfAbsent(key, idx)
      
    idx
  }
  
  /**
   * Returns the column index (if any) for a key.
   * 
   * If the key is a duplicate header then a FlatFileReaderException.DuplicateHeaders exception is throw.
   */
  final def colIndexForKey(key: String): Int = {
    val idx: Int = cachedRawColIndexForKey(key)
    
    if (-1 == idx) throw new FlatFileReaderException.DuplicateHeaders(key, "Found Duplicate Header: "+key+"  All Headers: "+headers)
    if (Int.MinValue == idx) throw new NoSuchElementException("Missing Column: '"+key+"'")

    require(idx >= 0)
    
    idx
  }
  
  /**
   * Returns the column index (if any) for a key.
   * 
   * If the key is a duplicate header then a FlatFileReaderException.DuplicateHeaders exception is throw.
   */
  final def getColIndexForKey(key: String): Option[Int] = {
    val idx: Int = cachedRawColIndexForKey(key)
    
    if (-1 == idx) throw new FlatFileReaderException.DuplicateHeaders(key, "Found Duplicate Header: "+key+"  All Headers: "+headers)
    
    if (Int.MinValue == idx) None
    else { require(idx >= 0); Some(idx) }
  }
  
  // Original implementation
//  def getColIndexForKey(key: String): Option[Int] = {
//    val idx: Option[Int] = headerMap.get(key) orElse normalHeaderMap.get(Normalize.lowerAlphanumeric(key))
//    if (idx.isDefined && idx.get == -1) throw new FlatFileReaderException.DuplicateHeaders(key, "Found Duplicate Header: "+key+"  All Headers: "+headers)
//    idx
//  }
  
  final def equalsNormalized(other: FlatFileRowHeaders): Boolean = equalsNormalized(other.headers)
  
  final def equalsNormalized(other: IndexedSeq[String]): Boolean = {
    (headers zip FlatFileRowHeaders.cleanHeaderValues(other)).forall{ case (a, b) => 
      Normalize.lowerAlphanumeric(a) == Normalize.lowerAlphanumeric(b)
    }
  }
}