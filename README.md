# Hmpps manage adjudications API
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-manage-adjudications-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-manage-adjudications-api)
[![Docker](https://quay.io/repository/hmpps/hmpps-manage-adjudications-api/status)](https://quay.io/repository/hmpps/hmpps-manage-adjudications-api/status)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://manage-adjudications-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config)

# Features
* Start a new draft adjudication

The frontend can be found here: <https://github.com/ministryofjustice/hmpps-manage-adjudications>

# Instructions
###Tests
Before running the tests `docker-compose -f docker-compose-postgres.yaml up` needs to be running and to have finished loading
before you start running the tests. Once done you can run the tests by running `./gradlew build`

###Running locally
`./gradlew bootRun --args='--spring.profiles.active=local,stdout'`

