# 5. Publish self-contained loss-of-visits state

[Next >>](9999-end.md)

Date: 2026-07-16

## Status

Accepted

## Context

The Visits service needs to react when an adjudication awards, changes, removes, quashes, or restores a social-visits punishment. The sanctions are available only for Adult Rule 51 adjudications and are modelled as two distinct punishment values: restriction and forfeiture. The standard adjudication event envelope contains the prisoner, charge, prison, and adjudication status, but status alone cannot distinguish these transitions. It also does not contain the punishment period, which would force Visits to call the reviewer-facing adjudications API and understand the wider punishment model.

The existing `GET /reported-adjudications/{chargeNumber}/v2` endpoint is not an integration contract for Visits: it requires reviewer access and an active prison caseload. A callback would therefore add runtime coupling and require a new service-facing read API.

## Decision

Publish one dedicated HMPPS domain event:

`adjudication.punishments.lossOfVisits`

The standard envelope remains at version `1.0`. Its `additionalInformation` contains the existing fields plus a `lossOfVisits` object:

```json
{
  "eventType": "adjudication.punishments.lossOfVisits",
  "version": "1.0",
  "occurredAt": "2026-07-16T10:15:30+01:00",
  "additionalInformation": {
    "chargeNumber": "MDI-001234",
    "prisonerNumber": "A1234BC",
    "prisonId": "MDI",
    "status": "CHARGE_PROVED",
    "lossOfVisits": {
      "changeType": "UPDATED",
      "punishments": [
        {
          "punishmentId": 42,
          "type": "RESTRICTION_OF_SOCIAL_VISITS",
          "duration": 28,
          "measurement": "DAYS",
          "startDate": "2026-07-16",
          "endDate": "2026-08-12",
          "hasChildUnder18": true
        }
      ]
    }
  }
}
```

`punishments` is the complete post-change social-visits punishment snapshot for the charge, not a delta. Optional schedule fields are omitted when they do not apply. A suspended punishment has `suspendedUntil` and no active start or end date. An activated punishment has its active dates and `activatedByChargeNumber`.

Active `startDate` and `endDate` values are inclusive. The API owns the Last Day calculation: it persists and publishes `endDate` as `startDate + duration - 1`. Clients may omit `endDate`; if they supply one for backwards compatibility, the API rejects it unless it matches the calculated value. This keeps stored and published periods authoritative and prevents the UI and API from calculating different dates.

The API rejects restriction or forfeiture on youth adjudications, requires an explicit `hasChildUnder18` value (including `false`), and applies the type-specific limits of 84 and 27 days respectively.

`changeType` has these meanings:

| Value | Meaning | Consumer action |
| --- | --- | --- |
| `AWARDED` | The charge gained its first social-visits punishment | Replace the charge state with the snapshot |
| `UPDATED` | Dates, duration, type, child answer, suspension, or activation changed | Replace the charge state with the snapshot |
| `REMOVED` | The charge no longer has a social-visits punishment | Remove the charge state; the snapshot is empty |
| `QUASHED` | The charge was quashed | Stop applying the charge state; the snapshot is retained for context |
| `UNQUASHED` | The quash was removed | Replace the charge state with the snapshot |

The event is raised from every backend route that can change effective visits state: punishment create/update/removal, rehabilitative-activity schedule changes, quash/unquash, and activation/deactivation of a suspended punishment. When activation changes a punishment owned by another charge, the event identifies and snapshots that original charge.

## Consumer rules and edge cases

- The reconciliation key is `(prisonerNumber, chargeNumber)`. Replacing charge state makes duplicate delivery idempotent.
- A prisoner can have overlapping punishments on multiple charges. Removing or quashing one charge must not clear restrictions belonging to another charge.
- Consumers should use `occurredAt` to reject an older snapshot received after a newer one.
- Natural expiry does not need a second event because the active end date is present in the snapshot.
- `hasChildUnder18` is recorded per social-visits punishment. Visits remains responsible for applying the policy exemption for visits from children.
- Activation must match the original suspended punishment's type, duration, and child answer. Partial activation cannot be zero, negative, or longer than the original social-visits punishment.
- `QUASHED` and `REMOVED` are explicit even when the adjudication status would appear to imply the transition; consumers do not infer action from `status`.

## Consequences

Visits can act without calling the Adjudications API or importing adjudication-specific rules. The event is larger than the minimal standard envelope, but remains small and bounded by the punishments on one charge. One event type and a full snapshot avoid ambiguous sequences when a single edit adds, removes, and changes punishments together.

The event uses the service's existing SNS publishing path and inherits its delivery guarantees; this decision does not introduce an outbox or new infrastructure. Registration in the shared HMPPS domain-events catalogue and any one-off backfill of punishments created before go-live remain release coordination tasks.
