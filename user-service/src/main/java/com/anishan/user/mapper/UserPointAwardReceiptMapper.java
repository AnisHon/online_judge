package com.anishan.user.mapper;

import com.anishan.user.domain.entity.UserPointAwardReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Explicit statements for the (user_id, problem_id) idempotency key. */
@Mapper
public interface UserPointAwardReceiptMapper {

    int insertReceipt(@Param("receipt") UserPointAwardReceipt receipt);

    UserPointAwardReceipt selectByUserAndProblem(@Param("userId") Long userId,
                                                 @Param("problemId") Long problemId);

    UserPointAwardReceipt selectByEventId(@Param("eventId") String eventId);
}
