Frugal Mechanic Flat File Reader
================================

[![Build Status](https://travis-ci.org/frugalmechanic/fm-flatfile.svg?branch=master)](https://travis-ci.org/frugalmechanic/fm-flatfile)

This is our TSV/CSV/Flat/Excel File Reader

Basic Usage
-----------

```scala
import fm.flatfile.{FlatFileReader, FlatFileRow}

// FlatFileReader implements LazySeq[FlatFileRow]
FlatFileReader("/path/to/file.tsv").foreach { row: FlatFileRow =>
  println("Column 1: "+row("Column 1"))
  println("Column 2: "+row("Column 2"))
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
