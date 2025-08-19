//package com.bilibili.flink.manager.dao.datasource;
//
//import com.alibaba.druid.pool.DruidDataSource;
//import com.alibaba.druid.support.http.StatViewServlet;
//import com.alibaba.druid.support.http.WebStatFilter;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.mybatis.spring.SqlSessionFactoryBean;
//import org.mybatis.spring.SqlSessionTemplate;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.boot.web.servlet.ServletRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.jdbc.datasource.DataSourceTransactionManager;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import javax.sql.DataSource;
//import java.sql.SQLException;
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class DruidConfig {
//    private Logger log = LoggerFactory.getLogger(DruidConfig.class);
//
//    @Value("${spring.datasource.url}")
//    private String dbUrl;
//
//    @Value("${spring.datasource.username}")
//    private String username;
//
//    @Value("${spring.datasource.password}")
//    private String password;
//
//    @Value("${spring.datasource.driver-class-name}")
//    private String driverClassName;
//
//    @Value("${spring.datasource.initialSize}")
//    private int initialSize;
//
//    @Value("${spring.datasource.minIdle}")
//    private int minIdle;
//
//    @Value("${spring.datasource.maxActive}")
//    private int maxActive;
//
//    @Value("${spring.datasource.maxWait}")
//    private int maxWait;
//
//    @Value("${spring.datasource.timeBetweenEvictionRunsMillis}")
//    private int timeBetweenEvictionRunsMillis;
//
//    @Value("${spring.datasource.minEvictableIdleTimeMillis}")
//    private int minEvictableIdleTimeMillis;
//
//    @Value("${spring.datasource.validationQuery}")
//    private String validationQuery;
//
//    @Value("${spring.datasource.testWhileIdle}")
//    private boolean testWhileIdle;
//
//    @Value("${spring.datasource.testOnBorrow}")
//    private boolean testOnBorrow;
//
//    @Value("${spring.datasource.testOnReturn}")
//    private boolean testOnReturn;
//
//    @Value("${spring.datasource.filters}")
//    private String filters;
//
//    @Value("${spring.datasource.logSlowSql}")
//    private String logSlowSql;
//
//
//    @Value("${spring.datasource.clickhouse.read.url}")
//    private String readCkDbUrl;
//
//    @Value("${spring.datasource.clickhouse.read.user}")
//    private String readCkDbUser;
//
//    @Value("${spring.datasource.clickhouse.read.pwd}")
//    private String readCkDbPwd;
//
//    @Value("${spring.datasource.clickhouse.read.driver}")
//    private String readCkDriverClassName;
//
//
//    @Value("${spring.datasource.clickhouse.read.min-idle}")
//    private int readCkMinIdle;
//
//    @Value("${spring.datasource.clickhouse.read.max-active}")
//    private int readCkMaxActive;
//
//    @Value("${spring.datasource.clickhouse.read.max-wait}")
//    private int readCkMaxWait;
//
//    @Bean
//    public ServletRegistrationBean druidServlet() {
//        ServletRegistrationBean reg = new ServletRegistrationBean();
//        reg.setServlet(new StatViewServlet());
//        reg.addUrlMappings("/druid/*");
//        reg.addInitParameter("loginUsername", username);
//        reg.addInitParameter("loginPassword", password);
//        reg.addInitParameter("logSlowSql", logSlowSql);
//        return reg;
//    }
//
//    @Bean
//    public FilterRegistrationBean filterRegistrationBean() {
//        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
//        filterRegistrationBean.setFilter(new WebStatFilter());
//        filterRegistrationBean.addUrlPatterns("/*");
//        filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
//        filterRegistrationBean.addInitParameter("profileEnable", "true");
//        return filterRegistrationBean;
//    }
//
//    @Bean
//    public DataSource mysqlDataSource() {
//        DruidDataSource datasource = new DruidDataSource();
//        datasource.setUrl(dbUrl);
//        datasource.setUsername(username);
//        datasource.setPassword(password);
//        datasource.setDriverClassName(driverClassName);
//        datasource.setInitialSize(initialSize);
//        datasource.setMinIdle(minIdle);
//        datasource.setMaxActive(maxActive);
//        datasource.setMaxWait(maxWait);
//        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
//        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
//        datasource.setValidationQuery(validationQuery);
//        datasource.setTestWhileIdle(testWhileIdle);
//        datasource.setTestOnBorrow(testOnBorrow);
//        datasource.setTestOnReturn(testOnReturn);
//        try {
//            datasource.setFilters(filters);
//        } catch (SQLException e) {
//            log.error("druid configuration initialization filter", e);
//        }
//        return datasource;
//    }
//
//    @Bean
//    public DataSource readClickHouseDataSource() {
//        DruidDataSource datasource = new DruidDataSource();
//        datasource.setUrl(readCkDbUrl);
//        datasource.setUsername(readCkDbUser);
//        datasource.setPassword(readCkDbPwd);
//        datasource.setDriverClassName(readCkDriverClassName);
//        datasource.setInitialSize(initialSize);
//        datasource.setMinIdle(readCkMinIdle);
//        datasource.setMaxActive(readCkMaxActive);
//        datasource.setMaxWait(readCkMaxWait);
//        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
//        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
//        datasource.setValidationQuery(validationQuery);
//        datasource.setTestWhileIdle(testWhileIdle);
//        datasource.setTestOnBorrow(testOnBorrow);
//        datasource.setTestOnReturn(testOnReturn);
//        try {
//            datasource.setFilters(filters);
//        } catch (SQLException e) {
//            log.error("druid configuration initialization filter", e);
//        }
//        return datasource;
//    }
//
//    @Bean
//    public DataSource dynamicDataSource() {
//        DynamicDataSource dataSource = new DynamicDataSource();
//        dataSource.setDefaultTargetDataSource(mysqlDataSource());
//        Map<Object, Object> dataSourceMap = new HashMap<>(4);
//        dataSourceMap.put(DataSourceKey.MYSQL, mysqlDataSource());
//        dataSourceMap.put(DataSourceKey.READ_CLICKHOUSE, readClickHouseDataSource());
//        dataSource.setTargetDataSources(dataSourceMap);
//        return dataSource;
//    }
//
//
//    @Bean
//    public SqlSessionFactory sqlSessionFactory() throws Exception {
//        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
//        sqlSessionFactoryBean.setDataSource(dynamicDataSource());
//        //此处设置为了解决找不到mapper文件的问题
//        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
//        sqlSessionFactoryBean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
//        sqlSessionFactoryBean.getObject().getConfiguration().setCallSettersOnNulls(true);
//
//        return sqlSessionFactoryBean.getObject();
//    }
//
//    @Bean
//    public SqlSessionTemplate sqlSessionTemplate() throws Exception {
//        return new SqlSessionTemplate(sqlSessionFactory());
//    }
//
//    /**
//     * 事务管理
//     *
//     * @return 事务管理实例
//     */
//    @Bean
//    public PlatformTransactionManager platformTransactionManager() {
//        return new DataSourceTransactionManager(dynamicDataSource());
//    }
//}
