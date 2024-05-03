package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule

@Transactional
@Service
class ActivatedSuspendedRepairService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun repair(): Set<SuspendedPunishmentEvent> {
    val events: MutableSet<SuspendedPunishmentEvent> = mutableSetOf()

    reportedAdjudicationRepository.findByPunishmentsActivatedFromChargeNumberIsNotNull().forEach { child ->
      // track which punishments to remove outside of the loop
      val punishmentsToRemove = mutableListOf<Punishment>()
      child.getPunishments().filter { p -> p.activatedFromChargeNumber != null }.forEach { activated ->
        val latestSchedule = activated.schedule.latestSchedule()
        val activatedFrom = reportedAdjudicationRepository.findByChargeNumber(activated.activatedFromChargeNumber!!)
        // it could have been manually activated before migration with made up data so ignore if this happens,  nothing that can be done now
        activatedFrom?.let { parent ->

          // try and match as closely as possible.  easy to review end result in preprod, as to which records get left (if any).  Problem records will also have suspended until still set
          parent.getPunishments()
            .firstOrNull { it.activatedByChargeNumber == child.chargeNumber && it.type == activated.type && it.privilegeType == activated.privilegeType && it.suspendedUntil != null }?.let {
                punishmentToRepair ->
              punishmentToRepair.suspendedUntil = null
              punishmentToRepair.schedule.add(
                PunishmentSchedule(days = latestSchedule.days, startDate = latestSchedule.startDate, endDate = latestSchedule.endDate),
              )
              events.add(
                SuspendedPunishmentEvent(
                  agencyId = parent.originatingAgencyId,
                  chargeNumber = parent.chargeNumber,
                  status = parent.status,
                  prisonerNumber = parent.prisonerNumber,
                ),
              )
              punishmentsToRemove.add(activated)
            }
        }
      }
      if (punishmentsToRemove.isNotEmpty()) {
        punishmentsToRemove.forEach { child.removePunishment(it) }
        events.add(
          SuspendedPunishmentEvent(
            agencyId = child.originatingAgencyId,
            chargeNumber = child.chargeNumber,
            status = child.status,
            prisonerNumber = child.prisonerNumber,
          ),
        )
      }
    }

    return events
  }
}
