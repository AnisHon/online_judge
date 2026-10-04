-- Supports the cached per-user yearly submission heatmap without scanning all submissions.
use db_problem;
delimiter $$
drop procedure if exists migrate_profile_heatmap_index$$
create procedure migrate_profile_heatmap_index()
begin
    if not exists (
        select 1 from information_schema.statistics
        where table_schema = database() and table_name = 'submit_log'
          and index_name = 'submit_log_user_submit_time'
    ) then
        alter table submit_log add index submit_log_user_submit_time(user_id, submit_time);
    end if;
end$$
delimiter ;
call migrate_profile_heatmap_index();
drop procedure migrate_profile_heatmap_index;
