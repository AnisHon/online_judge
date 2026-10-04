-- This routine is installed and invoked only by migrate-solution-domain.sh
-- --phase copy after maintenance confirmations. It copies one bounded keyset batch.
-- Conflict comparison happens in the orchestrator before CALL; INSERT never updates
-- an existing target row. The source remains intact for verification and rollback.
USE db_content;

DROP PROCEDURE IF EXISTS content_solution_copy_batch;
DELIMITER $$
CREATE PROCEDURE content_solution_copy_batch(
    IN p_entity VARCHAR(40),
    IN p_after_a BIGINT,
    IN p_after_b BIGINT,
    IN p_until_a BIGINT,
    IN p_until_b BIGINT,
    IN p_after_event CHAR(36),
    IN p_until_event CHAR(36)
)
BEGIN
    DECLARE v_sql LONGTEXT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET @oj_after_a = p_after_a;
    SET @oj_after_b = p_after_b;
    SET @oj_until_a = p_until_a;
    SET @oj_until_b = p_until_b;
    SET @oj_after_event = p_after_event;
    SET @oj_until_event = p_until_event;

    CASE p_entity
      WHEN 'solution_explanation' THEN
        SET v_sql = 'INSERT INTO db_content.solution_explanation
          (solution_id,title,problem_id,user_id,top_up,`private`,del_flag,moderation_state,comments_open,like_count,comment_count,first_published_at,`version`,create_time,update_time)
          SELECT s.solution_id,s.title,s.problem_id,s.user_id,s.top_up,s.`private`,s.del_flag,s.moderation_state,s.comments_open,s.like_count,s.comment_count,s.first_published_at,s.`version`,s.create_time,s.update_time
          FROM db_problem.solution_explanation s
          WHERE (@oj_after_a IS NULL OR s.solution_id > @oj_after_a) AND s.solution_id <= @oj_until_a
            AND NOT EXISTS (SELECT 1 FROM db_content.solution_explanation d WHERE d.solution_id=s.solution_id)
          ORDER BY s.solution_id LIMIT 500';
      WHEN 'solution_explanation_content' THEN
        SET v_sql = 'INSERT INTO db_content.solution_explanation_content(solution_id,content)
          SELECT s.solution_id,s.content FROM db_problem.solution_explanation_content s
          WHERE (@oj_after_a IS NULL OR s.solution_id > @oj_after_a) AND s.solution_id <= @oj_until_a
            AND NOT EXISTS (SELECT 1 FROM db_content.solution_explanation_content d WHERE d.solution_id=s.solution_id)
          ORDER BY s.solution_id LIMIT 500';
      WHEN 'solution_like' THEN
        SET v_sql = 'INSERT INTO db_content.solution_like(solution_id,user_id,active,first_notified,created_at,updated_at)
          SELECT s.solution_id,s.user_id,s.active,s.first_notified,s.created_at,s.updated_at FROM db_problem.solution_like s
          WHERE (@oj_after_a IS NULL OR (s.solution_id,s.user_id) > (@oj_after_a,@oj_after_b))
            AND (s.solution_id,s.user_id) <= (@oj_until_a,@oj_until_b)
            AND NOT EXISTS (SELECT 1 FROM db_content.solution_like d WHERE d.solution_id=s.solution_id AND d.user_id=s.user_id)
          ORDER BY s.solution_id,s.user_id LIMIT 500';
      WHEN 'solution_comment' THEN
        SET v_sql = 'INSERT INTO db_content.solution_comment(comment_id,solution_id,user_id,root_id,parent_id,reply_to_user_id,content,state,deleted_by,deleted_at,like_count,reply_count,client_request_id,created_at,updated_at)
          SELECT s.comment_id,s.solution_id,s.user_id,s.root_id,s.parent_id,s.reply_to_user_id,s.content,s.state,s.deleted_by,s.deleted_at,s.like_count,s.reply_count,s.client_request_id,s.created_at,s.updated_at
          FROM db_problem.solution_comment s
          WHERE (@oj_after_a IS NULL OR s.comment_id > @oj_after_a) AND s.comment_id <= @oj_until_a
            AND NOT EXISTS (SELECT 1 FROM db_content.solution_comment d WHERE d.comment_id=s.comment_id)
          ORDER BY s.comment_id LIMIT 500';
      WHEN 'comment_like' THEN
        SET v_sql = 'INSERT INTO db_content.comment_like(comment_id,user_id,active,first_notified,created_at,updated_at)
          SELECT s.comment_id,s.user_id,s.active,s.first_notified,s.created_at,s.updated_at FROM db_problem.comment_like s
          WHERE (@oj_after_a IS NULL OR (s.comment_id,s.user_id) > (@oj_after_a,@oj_after_b))
            AND (s.comment_id,s.user_id) <= (@oj_until_a,@oj_until_b)
            AND NOT EXISTS (SELECT 1 FROM db_content.comment_like d WHERE d.comment_id=s.comment_id AND d.user_id=s.user_id)
          ORDER BY s.comment_id,s.user_id LIMIT 500';
      WHEN 'solution_moderation_action' THEN
        SET v_sql = 'INSERT INTO db_content.solution_moderation_action(action_id,solution_id,target_type,target_id,author_id,operator_id,action,reason,created_at)
          SELECT s.action_id,s.solution_id,s.target_type,s.target_id,s.author_id,s.operator_id,s.action,s.reason,s.created_at FROM db_problem.solution_moderation_action s
          WHERE (@oj_after_a IS NULL OR s.action_id > @oj_after_a) AND s.action_id <= @oj_until_a
            AND NOT EXISTS (SELECT 1 FROM db_content.solution_moderation_action d WHERE d.action_id=s.action_id)
          ORDER BY s.action_id LIMIT 500';
      WHEN 'community_outbox' THEN
        SET v_sql = 'INSERT INTO db_content.content_event_outbox(event_id,dedupe_key,event_type,exchange_name,routing_key,payload,status,attempts,next_attempt_at,lease_owner,lease_until,last_error,created_at,sent_at)
          SELECT s.event_id,s.dedupe_key,s.event_type,s.exchange_name,s.routing_key,s.payload,s.status,s.attempts,s.next_attempt_at,s.lease_owner,s.lease_until,s.last_error,s.created_at,s.sent_at
          FROM db_problem.problem_event_outbox s
          WHERE s.event_type IN (''SOLUTION_PUBLISHED'',''SOLUTION_LIKED'',''SOLUTION_COMMENTED'',''COMMENT_REPLIED'',''COMMENT_LIKED'',''SOLUTION_MODERATED'',''COMMENT_MODERATED'')
            AND (@oj_after_event IS NULL OR s.event_id > @oj_after_event) AND s.event_id <= @oj_until_event
            AND NOT EXISTS (SELECT 1 FROM db_content.content_event_outbox d WHERE d.event_id=s.event_id)
          ORDER BY s.event_id LIMIT 500';
      ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unknown content solution transfer entity';
    END CASE;

    START TRANSACTION;
    SET @oj_solution_copy_sql = v_sql;
    PREPARE oj_solution_copy_stmt FROM @oj_solution_copy_sql;
    EXECUTE oj_solution_copy_stmt;
    DEALLOCATE PREPARE oj_solution_copy_stmt;
    COMMIT;
END$$
DELIMITER ;
