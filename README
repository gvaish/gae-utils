Google App Engine Utilities
===========================

Some of the utilities helpful at Google App Engine (GAE)

UnzipperServlet
---------------

One of the scenarios that people get uncomfortable with is
the 3000 file limit on GAE, noting that the non-JSP files
outside the /WEB-INF folder are counted twice on Java apps.

Based on a solution at [GAE Issue 161](http://code.google.com/p/googleappengine/issues/detail?id=161#c88), [UnzipperServlet](src/main/src/com/mastergaurav/gae/server/UnzippperServlet.java) was born.

Enlisted below are some of its features:

* Configure the zip file whose contents are to be served in web.xml as init-param
* Allow support for a welcome file. TODO: Add support for welcome file list
* Acknowledges the header `If-Modified-Since` and responds with `304` if need be
* Acknowledges the header `If-None-Match` and responds with `304` if need be
* Sends an `ETag` header. The checksum value is picked up from the md5 file.

Usage:

* For each file to be served by this servlet, create a md5 checksum file.
  Use `md5sum` utility to create the checksum. The file must be name
  original-file-name.md5
* Compress all the static files in a zip file
* Copy the zip file in a sub-folder in the WEB-INF/classes folder
* Register the path, with a leading `/` in web.xml using the `param-name` of `zipFile`
* Map the servlet to appropriate URL pattern
* Enjoy!


TODO:

* Allow custom expiration setting, similar to that in appengine-web.xml using
  the * and ** patterns


Contact
=======

Mail me at gaurav[dot]vaish[at]gmail[dot]com for any query / comments / critics. :)

