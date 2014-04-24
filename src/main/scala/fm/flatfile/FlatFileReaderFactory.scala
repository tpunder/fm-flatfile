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

import java.io.{File, InputStream}
import fm.common.{InputStreamResource, SingleUseResource}

trait FlatFileReaderFactory {
  
  final def apply(is: InputStream): FlatFileReader = apply(SingleUseResource(is))
  final def apply(is: InputStream, options: FlatFileReaderOptions): FlatFileReader = apply(SingleUseResource(is), options)
  
  final def apply(path: String): FlatFileReader = apply(new File(path))
  final def apply(path: String, options: FlatFileReaderOptions): FlatFileReader = apply(new File(path), options)
  
  final def apply(f: File): FlatFileReader = apply(InputStreamResource.forFileOrResource(f))
  final def apply(f: File, options: FlatFileReaderOptions): FlatFileReader = apply(InputStreamResource.forFileOrResource(f), options)
  
  final def apply(resource: InputStreamResource): FlatFileReader = apply(resource, FlatFileReaderOptions.default)
  
  def apply(resource: InputStreamResource, options: FlatFileReaderOptions): FlatFileReader
}