package com.bilibili.cluster.scheduler.dao.datasource;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class SpringConnectionFactory {

    /**
     * Inject this field to make sure the database is initialized, this can solve the table not found issue #8432.
     */
//    @Resource
//    public DataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer;

    @Value("${spring.datasource.clickhouse.read.driver}")
    private String ckDriverClassName;

    @Value("${spring.datasource.clickhouse.read.url}")
    private String ckUrl;

    @Value("${spring.datasource.clickhouse.read.user}")
    private String ckUser;

    @Value("${spring.datasource.clickhouse.read.pwd}")
    private String ckPwd;

    @Bean("mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("clickHouseDataSource")
    public DataSource clickHouseDataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDriverClassName(ckDriverClassName);
        hikariDataSource.setJdbcUrl(ckUrl);
        hikariDataSource.setUsername(ckUser);
        hikariDataSource.setPassword(ckPwd);
        hikariDataSource.setMaximumPoolSize(20);
        hikariDataSource.setAutoCommit(true);
        hikariDataSource.setIdleTimeout(5);
        return hikariDataSource;
    }

    @Primary
    @Bean("dynamicDataSource")
    public DataSource dynamicDataSource(@Qualifier("mysqlDataSource") DataSource mysqlDataSource,
                                        @Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
        DynamicDataSource dynamicRoutingDataSource = new DynamicDataSource();
        dynamicRoutingDataSource.setDefaultTargetDataSource(mysqlDataSource);

        Map<Object, Object> dataSourceMap = new HashMap<>(2);
        dataSourceMap.put(DataSourceKey.MYSQL, mysqlDataSource);
        dataSourceMap.put(DataSourceKey.READ_CLICK_HOUSE, clickHouseDataSource);
        dynamicRoutingDataSource.setTargetDataSources(dataSourceMap);
        return dynamicRoutingDataSource;
    }

    @Bean
    public MybatisPlusInterceptor paginationInterceptor(DbType dbType) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

    @Bean
    public DataSourceTransactionManager transactionManager(@Qualifier("dynamicDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dynamicDataSource") DataSource dataSource, GlobalConfig globalConfig,
                                               DbType dbType) throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(false);
        configuration.setCallSettersOnNulls(true);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        configuration.addInterceptor(paginationInterceptor(dbType));

        MybatisSqlSessionFactoryBean sqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sqlSessionFactoryBean.setConfiguration(configuration);
        sqlSessionFactoryBean.setDataSource(dataSource);

        sqlSessionFactoryBean.setGlobalConfig(globalConfig);
        sqlSessionFactoryBean.setTypeAliasesPackage("com.bilibili.cluster.scheduler.dao.entity");
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("mapper/*Mapper.xml"));
        sqlSessionFactoryBean.setDatabaseIdProvider(databaseIdProvider());
        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public GlobalConfig globalConfig() {
        return new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()
                .setIdType(IdType.AUTO)).setBanner(false);
    }

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("PostgreSQL", "pg");
        properties.setProperty("h2", "h2");
        properties.setProperty("clickhouse", "clickhouse");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }

    @Bean
    @Primary
    @Profile("mysql")
    public DbType mysql() {
        return DbType.MYSQL;
    }

    @Bean
    public DbType h2() {
        return DbType.H2;
    }

    @Bean
    @Profile("clickHouse")
    public DbType clickhouse() {
        return DbType.CLICK_HOUSE;
    }

    @Bean
    @Primary
    @Profile("postgresql")
    public DbType postgresql() {
        return DbType.POSTGRE_SQL;
    }
}
