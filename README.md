
# Hmpps manage adjudications API

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-manage-adjudications-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-manage-adjudications-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-manage-adjudications-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-manage-adjudications-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://manage-adjudications-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)
[![Repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.data%5B%3F%28%40.name%20%3D%3D%20%22hmpps-manage-adjudications-api%22%29%5D.status&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fgithub_repositories)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/github_repositories#hmpps-manage-adjudications-api "Link to report")

# Features
* Start a new draft adjudication

The frontend can be found here: <https://github.com/ministryofjustice/hmpps-manage-adjudications>

# Instructions

## Running locally

For running locally against docker instances of the following services:

- run this application independently e.g. in IntelliJ

`docker-compose -f docker-compose-local.yml up`

### Running all services including this service

`docker-compose up`

### Tests
Before running the tests `docker-compose up` needs to be running and to have finished loading
before you start running the tests. Once done you can run the tests by running `./gradlew build`

### Running locally
`./gradlew bootRun --args='--spring.profiles.active=dev-local'`

## Architecture

Architecture decision records start [here](doc/architecture/decisions/0001-use-adr.md)

## Glossary

There are numerous terms and acronyms used in this codebase that aren't immediately obvious, including

| Term     | Definition                                                                                          |
|----------|-----------------------------------------------------------------------------------------------------|
| HMPPS    | HM Prison and Probation Service, and executive agency of the MoJ                                    |
| MOJ      | Ministry of Justice                                                                                 |

## Licence
[MIT License](LICENSE)
