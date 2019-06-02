# Tyler

[![Apache License, Version 2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Build Status](https://travis-ci.org/marauder-io/tyler.svg?branch=master)](https://travis-ci.org/marauder-io/tyler)

The Tyler is a stateless component and provides a REST interface for vector tile storage maintenance.

In favor of it simplicity there is nothing else needed besides a running server oder docker environment.
One can use POST requests to add more geospatial features to the database. The tile endpoint can be used to build a mapping interface using Mapbox GLJS or OpenLayers.

Detailed documentation of the Marauder components coming soon on our webpage [https://marauder.io](https://marauder.io).

## Components

Besides the Tyler there are other Marauder components. We provide big data storage solutions focused on visualization and simple analytic tasks. The Following chart provides a simple overview.

![alt text](https://github.com/marauder-io/tyler/raw/master/resources/marauder-components.png "Marauder Component Chart")

Here you see a on demand built heatmap of over 1 billion entities spread of the united states. In detailed few the actual data can be explored while applying attribute or spatial filters.

![alt text](https://github.com/marauder-io/tyler/raw/master/resources/tank-demo.gif "Tank data exploration demo")
--------


For more information feel free to contact us (mail@marauder.io).

## Vector Tiling Server using KTOR

`application.kt` contains the main module to start the server application.

### Quick Start
`./gradlew run`

### Docker Quickstart

The last release is pushed to `:latest`. The `master` branch is always pushed to `:unstable`.  
`docker pull maraud3r/tyler:latest`  
`docker run --rm -it -p 23513:23513 maraud3r/tyler:latest`  

### REST API

* `POST /:layer?` accepts GeoJSON features separated by line to import, `geojson=true` indicates to import a GeoJSON file, features will be imported to given `layer` or the base layer
* `GET /:z/:x/:y.mvt` serves MapBox Vector tiles
* `DELETE /` clears the whole database
* `GET /static/index.html` provides a very simple visualization web application

### Configuration

The server can be configured using the `resources/application.conf` HOCON file.
Available storage types are `sqlite`, `mongo`, `fs`. 

Be aware the filesystem option (`fs`) utilizes the hard disk and can store millions of files.

### Hint

Multi-Layer is supported now. Keep in mind that multi-layer usage slows down the tile creating process.  

License
-------

Tyler is licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
