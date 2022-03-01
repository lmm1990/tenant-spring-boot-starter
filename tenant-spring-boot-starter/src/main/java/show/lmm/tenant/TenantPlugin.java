package show.lmm.tenant;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import show.lmm.tenant.config.TenantConfig;
import show.lmm.tenant.entity.BoundSqlSource;
import show.lmm.tenant.handler.TenantDataHandler;
import show.lmm.tenant.utils.sql.mysql.MySqlASTVisitor;

import java.util.List;
import java.util.Properties;

/**
 * mybatis多租户插件
 *
 * @author 刘明明
 * @since 2022-02-28 17:09:46
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class TenantPlugin implements Interceptor {

    @Autowired
    TenantConfig tenantConfig;
    @Autowired
    private MySqlASTVisitor mySqlASTVisitor;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation
                .getArgs()[0];

        //忽略租户字段
        String className = ms.getId().substring(0, ms.getId().lastIndexOf("."));
        if (TenantDataHandler.ignoreTenantfieldMethods.contains(className) || TenantDataHandler.ignoreTenantfieldMethods.contains(ms.getId())) {
            return invocation.proceed();
        }

        Object parameterObject = null;
        if (invocation.getArgs().length > 1) {
            parameterObject = invocation.getArgs()[1];
        }
        if (ms.getSqlCommandType() == SqlCommandType.UNKNOWN || ms.getSqlCommandType() == SqlCommandType.FLUSH) {
            return invocation.proceed();
        }
        final String sql = ms.getBoundSql(parameterObject).getSql();

        //改写sql
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        for (SQLStatement item : statements) {
            item.accept(mySqlASTVisitor);
        }
        resetSql2Invocation(invocation, statements);
        return invocation.proceed();
    }

    /**
     * 将新sql绑定到mybatis调用
     *
     * @param invocation: mybatis调用
     * @param statements:        sql语句
     * @since 刘明明/2021-09-02 13:53:33
     **/
    private void resetSql2Invocation(Invocation invocation, List<SQLStatement> statements) {
        final String sql = SQLUtils.toSQLString(statements, DbType.mysql);
        final Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        Object parameterObject = args[1];
        final BoundSql boundSql = statement.getBoundSql(parameterObject);

        // 重新new一个查询语句对像
        BoundSql newBoundSql = new BoundSql(statement.getConfiguration(), sql, boundSql.getParameterMappings(),
                parameterObject);
        // 把新的查询放到statement里
        MappedStatement newStatement = copyFromMappedStatement(statement, new BoundSqlSource(newBoundSql));

        for (ParameterMapping mapping : boundSql.getParameterMappings()) {
            String prop = mapping.getProperty();
            if (boundSql.hasAdditionalParameter(prop)) {
                newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
            }
        }
        args[0] = newStatement;
    }

    /**
     * 复制MappedStatement
     * @param ms
     * @param newSqlSource
     * @return
     */
    private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource,
                ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (String keyProperty : ms.getKeyProperties()) {
                keyProperties.append(keyProperty).append(",");
            }
            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
            builder.keyProperty(keyProperties.toString());
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}