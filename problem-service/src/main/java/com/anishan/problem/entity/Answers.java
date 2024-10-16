package com.anishan.problem.entity;

import cn.hutool.core.collection.CollectionUtil;
import com.anishan.commons.e.AnswerType;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@ApiModel("答案篮")
public class Answers {

    @Data
    @ApiModel("具体答案")
    public static class Answer {

        private AnswerType answerType;
        private List<String> answer;

        public boolean judge(String answer) {
            if (CollectionUtil.isEmpty(this.answer)) {
                return false;
            }

            boolean result = false;

            switch (answerType) {
                case AnyIn:
                    result = AnyInHandler(answer);
                    break;
                case Equal:
                    result = EqualHandler(answer);
                    break;
                case NotIn:
                    result = NotInHandler(answer);
                    break;
            }
            return result;
        }

        private boolean NotInHandler(String answer) {
            return !this.answer.contains(answer);
        }

        private boolean EqualHandler(String answer) {
            return Objects.equals(answer, this.answer.get(0));
        }

        private boolean AnyInHandler(String answer) {
            return this.answer.contains(answer);
        }
    }

    private Integer answerNumber;
    private Map<Integer, Answer> orderAnswers;
}
