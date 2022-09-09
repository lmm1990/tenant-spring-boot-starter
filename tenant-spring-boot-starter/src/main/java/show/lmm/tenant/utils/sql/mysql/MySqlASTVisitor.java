package show.lmm.tenant.utils.sql.mysql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.springframework.stereotype.Component;
import show.lmm.tenant.config.TenantConfig;
import show.lmm.tenant.handler.TenantDataHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * mysql ast解析器
 *
 * @author 刘明明
 * @since 2022-02-25 13:53
 */
@Component
public class MySqlASTVisitor extends MySqlASTVisitorAdapter {
    private final TenantConfig tenantConfig;

    public MySqlASTVisitor(TenantConfig tenantConfig) {
        this.tenantConfig = tenantConfig;
    }

    /**
     * 解析sql插入语句
     *
     * @param statement: sql插入对象
     * @return boolean
     * @since 2022-02-25 18:13:45
     **/
    public boolean visit(MySqlInsertStatement statement) {
        List<SQLExpr> columns = statement.getColumns();
        if (this.ignoreTableName(statement.getTableSource())) {
            return true;
        } else {
            Iterator var3 = columns.iterator();

            while (var3.hasNext()) {
                SQLExpr item = (SQLExpr) var3.next();
                if (item instanceof SQLIdentifierExpr) {
                    SQLIdentifierExpr column = (SQLIdentifierExpr) item;
                    if (this.tenantConfig.getFieldName().equals(column.getName())) {
                        return true;
                    }
                }
            }

            statement.addColumn(SQLUtils.toMySqlExpr(this.tenantConfig.getFieldName()));
            statement.getValuesList().forEach((valueItem) -> {
                valueItem.addValue(TenantDataHandler.getTenantFieldValue());
            });
            return true;
        }
    }

    /**
     * 解析sql修改语句
     *
     * @param statement: sql修改对象
     * @return boolean
     * @since 2022-02-25 18:21:17
     **/
    public boolean visit(MySqlUpdateStatement statement) {
        statement.getItems().forEach((item) -> {
            item.getValue().getChildren().forEach((sqlObject) -> {
                if (!(sqlObject instanceof SQLIdentifierExpr) && !(sqlObject instanceof SQLIntegerExpr) && !(sqlObject instanceof SQLVariantRefExpr)) {
                    SQLSelectQueryBlock selectQueryBlock = ((SQLSelect) sqlObject).getQueryBlock();
                    SQLTableSource sqlTableSource = selectQueryBlock.getFrom();
                    SQLExpr where = selectQueryBlock.getWhere();
                    if (!this.hasTenantField(sqlTableSource, where)) {
                        selectQueryBlock.addWhere(this.getAddWhereSQLExpr(sqlTableSource.getAlias()));
                    }

                }
            });
        });
        SQLTableSource sqlTableSource = statement.getTableSource();
        SQLExpr where = statement.getWhere();
        if (!this.hasTenantField(sqlTableSource, where)) {
            statement.addWhere(this.getAddWhereSQLExpr(sqlTableSource.getAlias()));
        }

        return true;
    }

    /**
     * 解析sql删除语句
     *
     * @param deleteStatement: sql删除对象
     * @return boolean
     * @since 2022-02-25 18:21:17
     **/
    public boolean visit(MySqlDeleteStatement deleteStatement) {
        SQLTableSource deleteSqlTableSource = deleteStatement.getFrom() == null ? deleteStatement.getTableSource() : deleteStatement.getFrom();
        SQLExpr where = deleteStatement.getWhere();
        Collection<SQLExpr> whereParams = this.getAddSqlJoinTenantFieldWhereParam(deleteSqlTableSource, where);
        if (whereParams != null) {
            whereParams.forEach((item) -> {
                deleteStatement.addWhere(item);
            });
            return true;
        } else {
            if (!this.hasTenantField(deleteSqlTableSource, where)) {
                deleteStatement.addWhere(this.getAddWhereSQLExpr(deleteSqlTableSource.getAlias()));
            }

            return true;
        }
    }

    /**
     * 解析sql查询语句
     *
     * @param statement: sql查询对象
     * @return boolean
     * @since 2022-02-25 13:57:57
     **/
    public boolean visit(SQLSelectStatement statement) {
        SQLSelectQueryBlock selectQueryBlock = statement.getSelect().getQueryBlock();
        SQLTableSource sqlTableSource = selectQueryBlock.getFrom();
        SQLExpr where = selectQueryBlock.getWhere();
        Collection<SQLExpr> whereParams = this.getAddSqlJoinTenantFieldWhereParam(sqlTableSource, where);
        if (whereParams != null) {
            whereParams.forEach((item) -> {
                statement.addWhere(item);
            });
            return true;
        } else {
            if (!this.hasTenantField(sqlTableSource, where)) {
                statement.addWhere(this.getAddWhereSQLExpr(sqlTableSource.getAlias()));
            }

            return true;
        }
    }

    /**
     * 查询添加连接查询租户字段where条件
     *
     * @param sqlTableSource sql
     * @param where          where条件
     * @since 2022-02-25 15:02:22
     **/
    private Collection<SQLExpr> getAddSqlJoinTenantFieldWhereParam(SQLTableSource sqlTableSource, SQLExpr where) {
        if (!(sqlTableSource instanceof SQLJoinTableSource)) {
            return null;
        } else {
            Collection<SQLExpr> whereParams = new HashSet();
            SQLJoinTableSource joinTableSource = (SQLJoinTableSource) sqlTableSource;
            SQLTableSource tableSource = joinTableSource.getLeft();
            if (!this.hasTenantField(tableSource, where)) {
                whereParams.add(this.getAddWhereSQLExpr(tableSource.getAlias()));
            }

            tableSource = joinTableSource.getRight();
            if (!this.hasTenantField(tableSource, where)) {
                whereParams.add(this.getAddWhereSQLExpr(tableSource.getAlias()));
            }

            return whereParams.isEmpty() ? null : whereParams;
        }
    }

    /**
     * 获得租户where条件
     *
     * @param tableNameAlias: 表名别名
     * @return com.alibaba.druid.sql.ast.SQLExpr
     * @since 2022-02-25 14:01:30
     **/
    private SQLExpr getAddWhereSQLExpr(String tableNameAlias) {
        if (tableNameAlias == null) {
            tableNameAlias = "";
        } else {
            tableNameAlias = String.format("%s.", tableNameAlias);
        }

        return SQLUtils.toMySqlExpr(String.format("%s%s=%d", tableNameAlias, this.tenantConfig.getFieldName(), TenantDataHandler.getTenantFieldValue()));
    }

    /**
     * 判断是否有租户字段
     *
     * @param tableSource: sql源码
     * @param where:       where条件
     * @return boolean
     * @since 2022-02-25 14:03:14
     **/
    private boolean hasTenantField(SQLTableSource tableSource, SQLExpr where) {
        if (where == null) {
            return this.ignoreTableName(tableSource);
        } else {
            String tableNameAlias = tableSource.getAlias();
            List<SQLObject> whereParams = where.getChildren();
            Iterator var5 = whereParams.iterator();

            while (var5.hasNext()) {
                SQLObject item = (SQLObject) var5.next();
                if (item instanceof SQLInSubQueryExpr) {
                    SQLInSubQueryExpr whereIn = (SQLInSubQueryExpr) item;
                    SQLSelect sqlSelect = whereIn.getSubQuery();
                    SQLExpr whereInfo = sqlSelect.getQueryBlock().getWhere();
                    tableSource = sqlSelect.getQueryBlock().getFrom();
                    if (!this.hasTenantField(tableSource, whereInfo)) {
                        tableNameAlias = sqlSelect.getQueryBlock().getFrom().getAlias();
                        sqlSelect.addWhere(this.getAddWhereSQLExpr(tableNameAlias));
                    }
                } else if (item instanceof SQLSelect) {
                    SQLSelect select = (SQLSelect) item;
                    SQLExpr whereInfo = select.getQueryBlock().getWhere();
                    tableSource = select.getQueryBlock().getFrom();
                    tableNameAlias = tableSource.getAlias();
                    if (!this.hasTenantField(tableSource, whereInfo)) {
                        select.addWhere(this.getAddWhereSQLExpr(tableNameAlias));
                    }
                } else {
                    String tenantFieldName;
                    if (item instanceof SQLBinaryOpExpr) {
                        SQLBinaryOpExpr whereItem = (SQLBinaryOpExpr) item;
                        tenantFieldName = tableNameAlias == null ? this.tenantConfig.getFieldName() : String.format("%s.%s", tableNameAlias, this.tenantConfig.getFieldName());
                        boolean hasTenantField = whereItem.getLeft().toString().contains(tenantFieldName);
                        if (!hasTenantField && whereItem.getOperator() == SQLBinaryOperator.BooleanAnd) {
                            hasTenantField = whereItem.getRight().toString().contains(tenantFieldName);
                        }

                        if (hasTenantField) {
                            return true;
                        }
                    } else if (item instanceof SQLInListExpr) {
                        SQLInListExpr whereItem = (SQLInListExpr) item;
                        tenantFieldName = tableNameAlias == null ? this.tenantConfig.getFieldName() : String.format("%s.%s", tableNameAlias, this.tenantConfig.getFieldName());
                        String whereItemFieldName = ((SQLIdentifierExpr) whereItem.getExpr()).getName();
                        boolean hasTenantField = whereItemFieldName.contains(tenantFieldName);
                        if (hasTenantField) {
                            return true;
                        }
                    }
                }
            }

            return this.ignoreTableName(tableSource);
        }
    }

    /**
     * 是否忽略表名
     *
     * @param tableSource sql源码
     * @return 是否忽略表名
     */
    private boolean ignoreTableName(SQLTableSource tableSource) {
        SQLExprTableSource finalTableSource = (SQLExprTableSource) tableSource;
        return finalTableSource == null ? true : this.tenantConfig.getIgnoreTableNames().contains(finalTableSource.getTableName());
    }
}