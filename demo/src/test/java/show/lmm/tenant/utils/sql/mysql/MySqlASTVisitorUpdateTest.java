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
 * mysql ast update测试
 *
 * @author 刘明明
 * @since 2022-02-28 10:32:41
 */
@SpringBootTest
public class MySqlASTVisitorUpdateTest {

    @Autowired
    private MySqlASTVisitor mySqlASTVisitor;

    /**
     * 测试update语句增加租户字段
     */
    @Test
    public void updateAddTenantField() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "update user_info set userName = '李四' where status = 1 and userId = 1";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "UPDATE user_info\n" +
                "SET userName = '李四'\n" +
                "WHERE status = 1\n" +
                "\tAND userId = 1\n" +
                "\tAND tenantId = 0");
    }

    /**
     * 测试update语句增加租户字段
     */
    @Test
    public void updateAddTenantField2() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "update user_info set userName = '李四' where status = 1 and userId = 1 and tenantId = 2";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "UPDATE user_info\n" +
                "SET userName = '李四'\n" +
                "WHERE status = 1\n" +
                "\tAND userId = 1\n" +
                "\tAND tenantId = 2");
    }

    /**
     * 测试update in语句增加租户字段
     */
    @Test
    public void updateAddTenantField3() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "update user_info a set labelName = (select labelName from label where status = 1 and labelId = a.labelId) where status = 1 and userId = 1";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "UPDATE user_info a\n" +
                "SET labelName = (\n" +
                "\tSELECT labelName\n" +
                "\tFROM label\n" +
                "\tWHERE status = 1\n" +
                "\t\tAND labelId = a.labelId\n" +
                "\t\tAND tenantId = 0\n" +
                ")\n" +
                "WHERE status = 1\n" +
                "\tAND userId = 1\n" +
                "\tAND a.tenantId = 0");
    }

    /**
     * 测试update in语句增加租户字段
     */
    @Test
    public void updateAddTenantField4() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "update user_info a set labelName = (select labelName from label where status = 1 and labelId = a.labelId) where status = 1 and userId = 1 and a.tenantId = 2";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "UPDATE user_info a\n" +
                "SET labelName = (\n" +
                "\tSELECT labelName\n" +
                "\tFROM label\n" +
                "\tWHERE status = 1\n" +
                "\t\tAND labelId = a.labelId\n" +
                "\t\tAND tenantId = 0\n" +
                ")\n" +
                "WHERE status = 1\n" +
                "\tAND userId = 1\n" +
                "\tAND a.tenantId = 2");
    }
}
