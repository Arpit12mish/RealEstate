create table otp_request_tracker (
    id bigserial primary key,
    phone_number varchar(20) not null,
    last_sent_at timestamptz null,
    cooldown_until timestamptz null,
    send_count_in_window integer not null default 0,
    send_window_start timestamptz null,
    failed_verify_count_in_window integer not null default 0,
    verify_window_start timestamptz null,
    blocked_until timestamptz null,
    last_verified_at timestamptz null
);

alter table otp_request_tracker
    add constraint uk_otp_tracker_phone unique (phone_number);

create index idx_otp_tracker_phone on otp_request_tracker(phone_number);
create index idx_otp_tracker_blocked_until on otp_request_tracker(blocked_until);
create index idx_otp_tracker_cooldown_until on otp_request_tracker(cooldown_until);