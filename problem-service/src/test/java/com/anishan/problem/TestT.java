package com.anishan.problem;


import com.anishan.problem.domain.entity.Problem;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@SpringBootTest
public class TestT {

    @Autowired
    private PlatformTransactionManager transactionManager;


    @SneakyThrows
    @Test
    public void test() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED); // 设置隔离级别
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);  // 设置传播行为

        // 2. 开始事务
        TransactionStatus status = transactionManager.getTransaction(def);

        Problem problem = new Problem();
        problem.setProblemId(1001L);
        problem.setTitle("卢本伟牛逼");
        Db.updateById(problem);
        System.out.println(Db.getById(1001, Problem.class));

        transactionManager.rollback(status);
    }


}
