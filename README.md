Frugal Mechanic Flat File Reader
================================

[![Build Status](https://travis-ci.org/frugalmechanic/fm-flatfile.svg?branch=master)](https://travis-ci.org/frugalmechanic/fm-flatfile)

This is our TSV/CSV/Flat/Excel File Reader

Basic Usage for Reading
-----------------------

```scala
import fm.flatfile.{FlatFileReader, FlatFileRow}

// FlatFileReader implements LazySeq[FlatFileRow]
FlatFileReader("/path/to/input.tsv").foreach { row: FlatFileRow =>
  println("Column 1: "+row("Column 1"))
  println("Column 2: "+row("Column 2"))
}

```

Basic Usage for Writing
-----------------------

```scala
import fm.common.FileOutputStreamResource
import fm.flatfile.{FlatFileWriter, FlatFileWriterOptions}
import java.io.File

// FlatFileReader implements LazySeq[FlatFileRow]
FlatFileWriter(FileOutputStreamResource(new File("/path/to/output.tsv")), FlatFileWriterOptions.CSV) { out: FlatFileWriter =>
  out.write("Column 1" -> "Row 1 Col 1 Value", "Column 2" -> "Row 1 Col 2 Value")
  out.write("Column 1" -> "Row 2 Col 1 Value", "Column 2" -> "Row 2 Col 2 Value")
  out.write("Column 1" -> "Row 3 Col 1 Value", "Column 2" -> "Row 3 Col 2 Value")
  out.write("Column 1" -> "Row 4 Col 1 Value", "Column 2" -> "Row 4 Col 2 Value")
}

```

Authors
-------

Tim Underwood (<a href="https://github.com/tpunder" rel="author">GitHub</a>, <a href="https://www.linkedin.com/in/tpunder" rel="author">LinkedIn</a>, <a href="https://twitter.com/tpunder" rel="author">Twitter</a>, <a href="https://plus.google.com/+TimUnderwood0" rel="author">Google Plus</a>)

Eric Peters (<a href="https://github.com/er1c" rel="author">GitHub</a>, <a href="https://www.linkedin.com/in/egpeters" rel="author">LinkedIn</a>, <a href="https://twitter.com/EricPeters" rel="author">Twitter</a>, <a href="https://plus.google.com/101943871346184224220" rel="author">Google Plus</a>)

Copyright
---------

Copyright [Frugal Mechanic](http://frugalmechanic.com)

License
-------

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
