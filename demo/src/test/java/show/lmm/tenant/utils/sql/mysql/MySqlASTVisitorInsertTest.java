package show.lmm.tenant.utils.sql.mysql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import show.lmm.tenant.handler.TenantDataHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * mysql ast insert测试
 *
 * @author 刘明明
 * @since 2022-02-28 10:31:32
 */
@SpringBootTest
public class MySqlASTVisitorInsertTest {

    @Autowired
    private MySqlASTVisitor mySqlASTVisitor;

    /**
     * 测试新增sql怎讲租户字段
     */
    @Test
    public void insertSqlAddTenantField() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "insert into user_info(userId,userName) values(1,'张三')";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "INSERT INTO user_info (userId, userName, tenantId)\n" +
                "VALUES (1, '张三', 0)");
    }

    /**
     * 测试新增sql怎讲租户字段
     */
    @Test
    public void insertSqlAddTenantField2() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "insert into user_info(userId,userName) values(1,'张三'),(2,'李四')";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "INSERT INTO user_info (userId, userName, tenantId)\n" +
                "VALUES (1, '张三', 0),\n" +
                "\t(2, '李四', 0)");
    }

    /**
     * 测试新增sql怎讲租户字段
     */
    @Test
    public void insertSqlAddTenantField3() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "insert into user_info(userId,userName,tenantId) values(1,'张三',2),(2,'李四',2)";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "INSERT INTO user_info (userId, userName, tenantId)\n" +
                "VALUES (1, '张三', 2),\n" +
                "\t(2, '李四', 2)");
    }
}
