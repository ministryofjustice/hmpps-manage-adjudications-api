# 3. Architecture Overview
[Next >>](0004-db-schema.md)


Date: 2023-11-29

## Status

Accepted

## Context
This document illustrates the high level architecture of the adjudication service.

## Architecture Overview
Below is the high level architecture of the adjudication service.  The key components are:  
- **Prison Staff** - The users of the service
- **Adjudication UI** - The UI application that allows prison staff to manage adjudications
- **Manage Adjudication API** - The API that provides the functionality to manage adjudications
- **Adjudication Insights API** - The API that provides the adjudication insights functionality
- **S3 Bucket** - The AWS S3 bucket that stores the adjudication insight data
- **Gotenberg API** - The API that provides the PDF rendering functionality
- **Adjudication Database** - The database that stores the adjudication data
- **Domain Events** - The events that are raised when an adjudication is created or updated
- **HMPPS Prisoner to NOMIS update** - The service that syncs adjudication data back to NOMIS
- **Oracle Forms** - The legacy screens that allow prison staff to manage prisoners
- **NOMIS Database** - The legacy database that holds the data for prisoners
- **Syscon Services** - The services that provide the migration and sync functionality for NOMIS (show in more detail below)
- **Event Infra** - AWS SNS/SQS pub/sub system that provides the event functionality for HMPPS
- **Prison API** - The service that provides prisoner data from NOMIS
- **Prisoner Search API** - The service that provides the prisoner search functionality
- **Audit Service** - The service that provides the audit functionality for DPS
- **Curious API** - The API service that provides neuro-diverse information
- **Data Science Platform** - Data science platform

```mermaid
flowchart TB
subgraph prisonStaff[Prison Staff]
    h1[-Person-]:::type
    d1[Prison Staff \n with NOMIS account]:::description
end
prisonStaff:::person

prisonStaff--Creates Adjudication -->uiApplication
prisonStaff--Creates Adjudication in NOMIS -->oracleForms

subgraph adjudicationReportingService[Adjudication Reporting Service]
    subgraph uiApplication[UI Application]
        direction LR
        h2[Container: Node / Typescript]:::type
        d2[Delivers the content for \n managing adjudication reports]:::description
    end
    uiApplication:::internalContainer

    subgraph adjudicationApi[Adjudication API]
        direction LR
        h5[Container: Kotlin / Spring Boot]:::type
        d5[Provides adjudication report \n functionality via a JSON API]:::description
    end
    adjudicationApi:::internalContainer
    
    subgraph adjudicationInsightsApi[Adjudication Insights API]
        direction LR
        h7[Container: Kotlin / Spring Boot]:::type
        d7[Provide AP data insights \n for adjudications]:::description
    end
    adjudicationInsightsApi:::internalContainer

    subgraph gotenbergApi[Gotenberg API]
        direction LR
        d8[Provide PDF rendering]:::description
    end
    gotenbergApi:::internalContainer
    
    subgraph database[Adjudication Database]
        direction LR
        h6[Container: Postgres Database Schema]:::type
        d6[Stores adjudications and reference data]:::description
    end
    database:::internalContainer

    subgraph s3Bucket[S3 Bucket]
        direction LR
        h611[Container: AWS S3]:::type
        d611[Stores adjudications insight data]:::description
    end
    s3Bucket:::awsService
    
    uiApplication--Makes API calls to -->adjudicationApi
    uiApplication--renders insight graphs/tables from API -->adjudicationInsightsApi
    adjudicationInsightsApi--reads insight data from -->s3Bucket
    uiApplication--renders PDF -->gotenbergApi
    adjudicationApi--Reads from and \n writes to -->database
end
adjudicationReportingService:::newSystem

uiApplication--Obtains neuro-diverse information -->curiousApi
uiApplication--Looks up & \n searches for prisoners -->prisonerSearchApi
adjudicationApi--Publishes adjudications \n created/updated events -->domainEvents
adjudicationApi--Audits changes -->auditService

domainEvents<--Listens to adjudication \n events from NOMIS-->adjudicationApi
    
subgraph otherServices[Other HMPPS Services]
    subgraph prisonApi[Prison API]
        direction LR
        h31[Container: Java/Kotlin / Spring Boot]:::type
        d31[Exposes NOMIS data]:::description
    end
    prisonApi:::internalContainer

    subgraph prisonerSearchApi[Prisoner Search API]
        direction LR
        h32[Container: Kotlin / Spring Boot]:::type
        d32[Prisoner Data Cache]:::description
    end
    prisonerSearchApi:::internalContainer
    
    subgraph auditService[Audit Service]
        direction LR
        h62[Container: Kotlin / Spring Boot]:::type
        d62[Receives and records audit events]:::description
    end
    auditService:::internalContainer
end
otherServices:::internalSystem

subgraph externalServices[External HMPPS Services]
   
    subgraph curiousApi[Curious API]
        direction LR
        d52[Exposes Neuro-diversity data ]:::description
    end
    curiousApi:::externalContainer
end
externalServices:::externalSystem

subgraph analyticalPlatform[Analytical Platform]

    subgraph dataSciencePlatform[Data Science Platform]
        direction LR
        d72[Aggregates Adjudication data \n into multi views/patterns ]:::description
    end
    dataSciencePlatform:::externalContainer
    
end
externalServices:::externalSystem

dataSciencePlatform -- Data Engineering Pipeline \n Pushes insight data into S3 --> s3Bucket
nomisDb--Pulls data from NOMIS on nightly basis --> dataSciencePlatform
domainEvents<--Listens to adjudication events from DPS -->sysconApis

subgraph eventsSystem[Event / Audit Services]
    subgraph domainEvents[Domain Events]
        direction LR
        h61[Container: SNS / SQS]:::type
        d61[Pub / Sub System]:::description
    end
    domainEvents:::internalContainer
end
eventsSystem:::internalSystem

prisonApi--Reads from and \n writes to -->nomisDb
uiApplication--get prison data -->prisonApi

subgraph NOMIS[NOMIS & Related Services]
    subgraph sysconApis[Syscon Services]
        direction LR
        h82[Container: Kotlin / Spring Boot]:::type
        d82[Migration and Sync Management Services]:::description
    end
    sysconApis:::sysconContainer
    subgraph oracleForms[NOMIS front end]
        direction LR
        h91[Container: Weblogic / Oracle Forms]:::type
        d91[Java applet screens surfacing NOMIS data]:::description
    end
    oracleForms:::legacyContainer
    
    subgraph nomisDb[NOMIS Database]
        direction LR
        h92[Container: Oracle 11g Database]:::type
        d92[Stores core \n information about prisoners, \n prisons, finance, etc]:::description
    end
    nomisDb:::legacyContainer

    oracleForms-- read/write data to -->nomisDb
    sysconApis-- read/write data to -->nomisDb
end
NOMIS:::legacySystem


%% Element type definitions

classDef person fill:#90BD90, color:#000
classDef internalContainer fill:#1168bd, color:#fff
classDef legacyContainer fill:purple, color:#fff
classDef externalContainer fill:#A890BD, color:#fff
classDef sysconContainer fill:#1168bd, color:#fff
classDef internalSystem fill:#A8B5BD
classDef externalSystem fill:#F8B5BD
classDef newSystem fill:#D5EAF6, color:#000
classDef legacySystem fill:#A890BD, color:#fff
classDef awsService fill:#545454, color:#fff


classDef type stroke-width:0px, color:#fff, fill:transparent, font-size:12px
classDef description stroke-width:0px, color:#fff, fill:transparent, font-size:13px
```

### Nomis to DPS Migration Services
These are maintained by Syscon and are used to migrate data from NOMIS to DPS.  The services are:
- **NOMIS Prisoner API** - The service that provides prisoner data from NOMIS
- **NOMIS Prisoner from NOMIS Migration** - The service that provides the migration functionality for NOMIS
- **NOMIS Mapping Service** - The service that provides the mapping functionality for NOMIS
- **History Record Database** - The database that stores the history of migrations and sync'd data
- **Mapping Database** - The database that stores the mapping data

```mermaid
flowchart TB
    subgraph nomisSyncSystem[NOMIS sync services]
        subgraph nomisPrisonerApi[HMPPS NOMIS Prisoner API]
            direction LR
            h11[Container: Kotlin / Spring Boot]:::type
            d11[Internal API for NOMIS data]:::description
        end
        nomisPrisonerApi:::migrationContainer

        subgraph nomisUpdateApi[HMPPS Prisoner to NOMIS update]
            direction LR
            h16[Container: Kotlin / Spring Boot]:::type
            d16[Sync functionality]:::description
        end
        nomisUpdateApi:::migrationContainer

        subgraph nomisMigrationApi[HMPPS Prisoner from NOMIS Migration]
            direction LR
            h12[Container: Kotlin / Spring Boot]:::type
            d12[Migration from NOMIS service]:::description
        end
        nomisMigrationApi:::migrationContainer

        subgraph nomisMappingService[HMPPS NOMIS Mapping Service]
            direction LR
            h13[Container: Kotlin / Spring Boot]:::type
            d13[Provides mapping between \n DPS and NOMIS]:::description
        end
        nomisMappingService:::migrationContainer

        subgraph historyDb[History Record Database]
            direction LR
            h14[Container: Postgres Database Schema]:::type
            d14[Stores history of migrations \n and sync'd data]:::description
        end
        historyDb:::migrationContainer

        subgraph mappingDb[Mapping Database]
            direction LR
            h15[Container: Postgres Database Schema]:::type
            d15[Stores mapping data]:::description
        end
        mappingDb:::migrationContainer

        nomisMigrationApi-- pull migration data from -->nomisPrisonerApi
        nomisMigrationApi-- check for existing mapping -->nomisMappingService
        nomisMigrationApi-- record history -->historyDb
        nomisMappingService-- read records -->mappingDb
        nomisUpdateApi--update NOMIS via -->nomisPrisonerApi


    end
    nomisSyncSystem:::otherHmppsSystem

    subgraph NOMIS[NOMIS]
        subgraph oracleForms[NOMIS front end]
            direction LR
            h91[Container: Weblogic / Oracle Forms]:::type
            d91[Java applet screens surfacing NOMIS data]:::description
        end
        oracleForms:::legacyContainer

        subgraph nomisDb[NOMIS Database]
            direction LR
            h92[Container: Oracle 11g Database]:::type
            d92[Stores core \n information about prisoners, \n prisons, finance, etc]:::description
        end
        nomisDb:::legacyContainer

        oracleForms-- read/write data to -->nomisDb
    end
    NOMIS:::legacySystem

    domainEvents<--Listens to adjudication events from DPS -->nomisUpdateApi
    nomisPrisonerApi--read and update -->nomisDb

    classDef migrationContainer fill:wheat
    classDef otherHmppsSystem fill: lightblue, color:#fff, stroke-width:0px  
    classDef legacyContainer fill:purple
    classDef internalSystem fill:lightblue
    classDef legacySystem fill:lightblue 

    classDef type stroke-width:0px, color:#fff, fill:transparent, font-size:12px
    classDef description stroke-width:0px, color:#fff, fill:transparent, font-size:13px
    
```
[Next >>](0004-db-schema.md)
