## Vector Tiling Server using KTOR

`application.kt` contains the main module to start the server application.

### Quick Start
`./gradlew run`

### REST API

* `POST /` accepts GeoJSON files to import, `gzip` compression is optional
* `GET /:z/:x/:y.mvt` serves MapBox Vector tiles
* `DELETE /` clears the whole database
* `GET /static/index.html` provides a very simple visualization web application

### Configuration

The server can be configured using the `resources/application.con` HOCON file.
Available storage types are `sqlite`, `mongo`, `fs`. 

Be aware the filesystem option (`fs`) utilizes the harddisk and can store millions of files.