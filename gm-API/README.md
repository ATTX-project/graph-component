## GM API

Current directory contains:
* WF-API implementation in `src/gm-API` folder
* Docker container and implementation for the WF API

VERSION: 0.1

### GM API Docker container

Using the WF API Docker container:
* build the container `docker build -t attxproject/gmapi .` in the current folder;
* running the container `docker run -d -p 4302:4302 attxproject/gmapi` runs the container in detached mode on the `4302` port (production version should have this port hidden);
* using the endpoints `http://localhost:4301/map` or `http://localhost:4302/clusterids`.

The version number is specified in `src/gm-API/gmapi.py` under `version` variable.


# GM API server

## Overview
The GM exposes information from the Graph Store to the Distribution component (Elasticsearch). It runs the mapping processing, clustering of IDs for the data and also it communicates with the Workflow API about the Provenance information in the Graph Store.

The GM API requires python 2.7 installed.

### Build with Gradle

Install [gradle](https://gradle.org/gradle-download/?_ga=1.226518941.1083404848.1481538559) and run `gradle wrapper`. The tasks available are listed below:

* clean: `./gradlew clean`
* build: `./gradlew build`
* other tasks: `./gradlew :runTests` or `./gradlew :gm-API:pytest`
* exclude tasks: `./gradlew build -x :gm-API:pytest`
* debugging: `./gradlew :gm-API:runTests --stacktrace`
* do all: `./gradlew clean build -x :gm-API:pytest -x :gm-API:runTests`
* see tasks: `./gradlew tasks` and depenencies `./gradlew depenencies`
* see test coverage `./gradlew :gm-API:pytest coverage` it will generate a html report in `build/coverage/htmlcov`

### Run without Gradle

To run the server, please execute the following (preferably in a virtual environment):

```
pip install -r requirements
python app.py
```
or `python src/gm-api/wfapi.py` in the `gm-API` folder

and open the browser to address in order to do the mapping:

```
http://localhost:4302/map
```

The Swagger definition lives here:`src/gm-api/swagger/gmAPI-swagger.yml`.


## Running Tests

In order work/generate tests:
* use the command: `py.test tests` in the `gm-API` folder
* coverage: `py.test --cov-report html --cov=gm-API tests/` in the `gm-API` folder
* generate cover report `py.test tests --html=build/test-report/index.html --self-contained-html` - this will generate a `build/test-report/index.html` folder that contains html based information about the tests coverage.
