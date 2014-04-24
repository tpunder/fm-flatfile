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

import fm.common.Resource
import scala.util.{Failure, Success, Try}
import java.io.{InputStream, Reader}

/**
 * This creates an instance of a FlatFileReader for a given implementation
 */
final class FlatFileReaderForImpl[IN](resource: Resource[IN], options: FlatFileReaderOptions, impl: FlatFileReaderImpl[IN]) extends FlatFileReader {
  final val withTries: FlatFileReaderWithTries = new FlatFileReaderWithTriesForImpl(resource, options, impl)
  
  def foreach[U](f: FlatFileRow => U): Unit = withTries.map{ _.get }.foreach(f)
}

final class FlatFileReaderWithTriesForImpl[IN](resource: Resource[IN], options: FlatFileReaderOptions, impl: FlatFileReaderImpl[IN]) extends FlatFileReaderWithTries {
  
  def foreach[U](f: Try[FlatFileRow] => U): Unit = resource.use { in: IN =>
    impl.foreach(in, options)(f)
  }
}