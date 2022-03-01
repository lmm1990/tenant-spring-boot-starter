package show.lmm.tenant.utils.sql.mysql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlExprImpl;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.springframework.stereotype.Component;
import show.lmm.tenant.config.TenantConfig;
import show.lmm.tenant.handler.TenantDataHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * mysql ast解析器
 *
 * @author 刘明明
 * @since 2022-02-25 13:53
 */
@Component
public class MySqlASTVisitor extends MySqlASTVisitorAdapter {

    private TenantConfig tenantConfig;

    public MySqlASTVisitor(TenantConfig tenantConfig) {
        this.tenantConfig = tenantConfig;
    }

    /**
     * 解析sql插入语句
     *
     * @param statement:  sql插入对象
     * @return boolean
     * @since 刘明明/2022-02-25 18:13:45
     **/
    public boolean visit(MySqlInsertStatement statement) {
        final List<SQLExpr> columns = statement.getColumns();
        //忽略表名
        if(ignoreTableName(statement.getTableSource())){
            return true;
        }
        for (SQLExpr item : columns){
            if(item instanceof SQLIdentifierExpr){
                final SQLIdentifierExpr column = (SQLIdentifierExpr)item;
                if(tenantConfig.getFieldName().equals(column.getName())){
                    return true;
                }
            }
        }
        statement.addColumn(SQLUtils.toMySqlExpr(tenantConfig.getFieldName()));
        statement.getValuesList().forEach((valueItem) -> {
            valueItem.addValue(TenantDataHandler.getTenantFieldValue());
        });
        return true;
    }

    /**
     * 解析sql修改语句
     *
     * @param statement:  sql修改对象
     * @return boolean
     * @since 刘明明/2022-02-25 18:21:17
     **/
    public boolean visit(MySqlUpdateStatement statement) {
        statement.getItems().forEach((item)->{
            item.getValue().getChildren().forEach((sqlObject -> {
                final SQLSelectQueryBlock selectQueryBlock = ((SQLSelect) sqlObject).getQueryBlock();
                final SQLTableSource sqlTableSource = selectQueryBlock.getFrom();
                final SQLExpr where  = selectQueryBlock.getWhere();
                if (!hasTenantField(sqlTableSource,where)) {
                    selectQueryBlock.addWhere(getAddWhereSQLExpr(sqlTableSource.getAlias()));
                }
            }));
        });
        final SQLTableSource sqlTableSource = statement.getTableSource();
        final SQLExpr where = statement.getWhere();
        if (!hasTenantField(sqlTableSource,where)) {
            statement.addWhere(getAddWhereSQLExpr(sqlTableSource.getAlias()));
        }
        return true;
    }


    /**
     * 解析sql删除语句
     *
     * @param deleteStatement:  sql删除对象
     * @return boolean
     * @since 刘明明/2022-02-25 18:21:17
     **/
    public boolean visit(MySqlDeleteStatement deleteStatement) {
        final SQLTableSource deleteSqlTableSource = deleteStatement.getFrom()==null? deleteStatement.getTableSource() : deleteStatement.getFrom();
        final SQLExpr where = deleteStatement.getWhere();
        Collection<SQLExpr> whereParams = getAddSqlJoinTenantFieldWhereParam(deleteSqlTableSource, where);
        if (whereParams!=null) {
            whereParams.forEach((item)->{
                deleteStatement.addWhere(item);
            });
            return true;
        }
        if (!hasTenantField(deleteSqlTableSource,where)) {
            deleteStatement.addWhere(getAddWhereSQLExpr(deleteSqlTableSource.getAlias()));
        }
        return true;
    }

    /**
     * 解析sql查询语句
     *
     * @param statement: sql查询对象
     * @return boolean
     * @since 刘明明/2022-02-25 13:57:57
     **/
    public boolean visit(SQLSelectStatement statement) {
        final SQLSelectQueryBlock selectQueryBlock = statement.getSelect().getQueryBlock();
        final SQLTableSource sqlTableSource = selectQueryBlock.getFrom();
        final SQLExpr where = selectQueryBlock.getWhere();
        Collection<SQLExpr> whereParams = getAddSqlJoinTenantFieldWhereParam(sqlTableSource, where);
        if (whereParams!=null) {
            whereParams.forEach((item)->{
                statement.addWhere(item);
            });
            return true;
        }
        if (!hasTenantField(sqlTableSource,where)) {
            statement.addWhere(getAddWhereSQLExpr(sqlTableSource.getAlias()));
        }
        return true;
    }

    /**
     * 查询添加连接查询租户字段where条件
     *
     * @param sqlTableSource sql
     * @param where          where条件
     * @since 刘明明/2022-02-25 15:02:22
     **/
    private Collection<SQLExpr> getAddSqlJoinTenantFieldWhereParam(SQLTableSource sqlTableSource, SQLExpr where) {
        if (!(sqlTableSource instanceof SQLJoinTableSource)) {
            return null;
        }
        Collection<SQLExpr> whereParams = new HashSet<>();
        final SQLJoinTableSource joinTableSource = (SQLJoinTableSource) sqlTableSource;
        //left
        SQLTableSource tableSource =joinTableSource.getLeft();
        if (!hasTenantField(tableSource,where)) {
            whereParams.add(getAddWhereSQLExpr(tableSource.getAlias()));
        }
        //right
        tableSource = joinTableSource.getRight();
        if (!hasTenantField(tableSource,where)) {
            whereParams.add(getAddWhereSQLExpr(tableSource.getAlias()));
        }
        if(whereParams.isEmpty()){
            return null;
        }
        return whereParams;
    }

    /**
     * 获得租户where条件
     *
     * @param tableNameAlias: 表名别名
     * @return com.alibaba.druid.sql.ast.SQLExpr
     * @since 刘明明/2022-02-25 14:01:30
     **/
    private SQLExpr getAddWhereSQLExpr(String tableNameAlias) {
        if (tableNameAlias == null) {
            tableNameAlias = "";
        } else {
            tableNameAlias = String.format("%s.", tableNameAlias);
        }
        return SQLUtils.toMySqlExpr(String.format("%s%s=%d", tableNameAlias, tenantConfig.getFieldName(), TenantDataHandler.getTenantFieldValue()));
    }

    /**
     * 判断是否有租户字段
     *
     * @param tableSource: sql源码
     * @param where: where条件
     * @return boolean
     * @since 刘明明/2022-02-25 14:03:14
     **/
    private boolean hasTenantField(SQLTableSource tableSource,SQLExpr where) {
        if(where==null){
            return ignoreTableName(tableSource);
        }
        String tableNameAlias = tableSource.getAlias();
        final List<SQLObject> whereParams = where.getChildren();
        for (SQLObject item : whereParams) {
            if (item instanceof SQLInSubQueryExpr) {
                final SQLInSubQueryExpr whereIn = (SQLInSubQueryExpr) item;
                final SQLSelect sqlSelect = whereIn.getSubQuery();
                final SQLExpr whereInfo = sqlSelect.getQueryBlock().getWhere();
                tableSource = sqlSelect.getQueryBlock().getFrom();
                if (!hasTenantField(tableSource,whereInfo)) {
                    tableNameAlias = sqlSelect.getQueryBlock().getFrom().getAlias();
                    sqlSelect.addWhere(getAddWhereSQLExpr(tableNameAlias));
                }
            } else if (item instanceof SQLSelect) {
                final SQLSelect select = (SQLSelect) item;
                final SQLExpr whereInfo = select.getQueryBlock().getWhere();
                tableSource = select.getQueryBlock().getFrom();
                tableNameAlias = tableSource.getAlias();
                if (!hasTenantField(tableSource,whereInfo)) {
                    select.addWhere(getAddWhereSQLExpr(tableNameAlias));
                }
            } else if (item instanceof SQLBinaryOpExpr) {
                final SQLBinaryOpExpr whereItem = (SQLBinaryOpExpr) item;
                final String tenantFieldName = tableNameAlias==null?tenantConfig.getFieldName():
                        String.format("%s.%s",tableNameAlias,tenantConfig.getFieldName());
                if (whereItem.getLeft().toString().contains(tenantFieldName)) {
                    return true;
                }
            }
        }
        //忽略表名
        return ignoreTableName(tableSource);
    }

    /**
     * 是否忽略表名
     * @param tableSource sql源码
     * @return 是否忽略表名
     */
    private boolean ignoreTableName(SQLTableSource tableSource){
        final SQLExprTableSource finalTableSource = (SQLExprTableSource)tableSource;
        return tenantConfig.getIgnoreTableNames().contains(finalTableSource.getTableName());
    }
}