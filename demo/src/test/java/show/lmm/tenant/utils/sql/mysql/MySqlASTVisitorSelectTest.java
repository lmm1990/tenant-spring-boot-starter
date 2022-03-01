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
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * mysql ast 查询测试
 *
 * @author 刘明明
 * @since 2022-02-28 10:30:23
 */
@SpringBootTest
public class MySqlASTVisitorSelectTest {

    @Autowired
    private MySqlASTVisitor mySqlASTVisitor;

    /**
     * 测试简单查询增加租户字段
     */
    @Test
    public void simpleSelectAddTenantField() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "select userId,userName from user_info where status = 1";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "SELECT userId, userName\n" +
                "FROM user_info\n" +
                "WHERE status = 1\n" +
                "\tAND tenantId = 0");
    }

    /**
     * 测试简单查询增加租户字段
     */
    @Test
    public void simpleSelectAddTenantField2() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "select userId,userName from user_info where status = 1 and tenantId = 2";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "SELECT userId, userName\n" +
                "FROM user_info\n" +
                "WHERE status = 1\n" +
                "\tAND tenantId = 2");
    }

    /**
     * 测试连接查询增加租户字段
     */
    @Test
    public void joinSelectAddTenantField() {
        TenantDataHandler.setTenantFieldValue(0L);
        String sql = "select a.userId,a.userName from user_info a left join hobby b on a.userId = b.userId where status = 1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        final String compareSql1 = "SELECT a.userId, a.userName\n" +
                "FROM user_info a\n" +
                "\tLEFT JOIN hobby b ON a.userId = b.userId\n" +
                "WHERE status = 1\n" +
                "\tAND a.tenantId = 0\n" +
                "\tAND b.tenantId = 0";
        final String compareSql2 = "SELECT a.userId, a.userName\n" +
                "FROM user_info a\n" +
                "\tLEFT JOIN hobby b ON a.userId = b.userId\n" +
                "WHERE status = 1\n" +
                "\tAND b.tenantId = 0\n" +
                "\tAND a.tenantId = 0";
        assertTrue(finalSql.equals(compareSql1) || finalSql.equals(compareSql2));
    }

    /**
     * 测试连接查询增加租户字段
     */
    @Test
    public void joinSelectAddTenantField2() {
        TenantDataHandler.setTenantFieldValue(0L);
        String sql = "select a.userId,a.userName from user_info a left join hobby b on a.userId = b.userId where status = 1 and a.tenantId=1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "SELECT a.userId, a.userName\n" +
                "FROM user_info a\n" +
                "\tLEFT JOIN hobby b ON a.userId = b.userId\n" +
                "WHERE status = 1\n" +
                "\tAND a.tenantId = 1\n" +
                "\tAND b.tenantId = 0");
    }

    /**
     * 测试子查询增加租户字段
     */
    @Test
    public void inSelectAddTenantField() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "select roleId,roleName from role a where roleId in(select roleId from user_role_relation b where userId = 1)";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "SELECT roleId, roleName\n" +
                "FROM role a\n" +
                "WHERE roleId IN (\n" +
                "\t\tSELECT roleId\n" +
                "\t\tFROM user_role_relation b\n" +
                "\t\tWHERE userId = 1\n" +
                "\t\t\tAND b.tenantId = 0\n" +
                "\t)\n" +
                "\tAND a.tenantId = 0");
    }

    /**
     * 测试子查询增加租户字段
     */
    @Test
    public void inSelectAddTenantField2() {
        TenantDataHandler.setTenantFieldValue(0L);
        final String sql = "select roleId,roleName from role a where roleId in(select roleId from user_role_relation b where userId = 1 and b.tenantId = 1)";
        final List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        final String finalSql = SQLUtils.toSQLString(statements, DbType.mysql);
        System.out.println(finalSql);
        assertEquals(finalSql, "SELECT roleId, roleName\n" +
                "FROM role a\n" +
                "WHERE roleId IN (\n" +
                "\t\tSELECT roleId\n" +
                "\t\tFROM user_role_relation b\n" +
                "\t\tWHERE userId = 1\n" +
                "\t\t\tAND b.tenantId = 1\n" +
                "\t)\n" +
                "\tAND a.tenantId = 0");
    }
}
