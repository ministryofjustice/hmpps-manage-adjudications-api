create index booking_agency_adjudications_idx on reported_adjudications (offender_booking_id, originating_agency_id,
                                                                         override_agency_id, date_time_of_discovery,
                                                                         status);