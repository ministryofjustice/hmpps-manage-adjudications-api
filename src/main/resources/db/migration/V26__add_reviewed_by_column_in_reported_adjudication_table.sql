alter table reported_adjudications
    add column review_user_id varchar(32);

update reported_adjudications
set review_user_id = modify_user_id
where status is not null
  and status != 'AWAITING_REVIEW'
