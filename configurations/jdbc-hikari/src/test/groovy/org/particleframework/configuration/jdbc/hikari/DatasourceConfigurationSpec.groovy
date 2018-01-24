package org.particleframework.configuration.jdbc.hikari

import com.zaxxer.hikari.HikariDataSource
import org.particleframework.context.ApplicationContext
import org.particleframework.context.DefaultApplicationContext
import org.particleframework.context.env.MapPropertySource
import org.particleframework.inject.qualifiers.Qualifiers
import spock.lang.Ignore
import spock.lang.Specification

import java.sql.ResultSet

class DatasourceConfigurationSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(HikariDataSource)
        !applicationContext.containsBean(DatasourceConfiguration)

        cleanup:
        applicationContext.close()
    }

    void "test blank configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(HikariDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        HikariDataSource dataSource = applicationContext.getBean(HikariDataSource)

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.jdbcUrl == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }

    void "test operations with a blank connection"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(HikariDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        HikariDataSource dataSource = applicationContext.getBean(HikariDataSource)
        ResultSet resultSet = dataSource.getConnection().prepareStatement("SELECT H2VERSION() FROM DUAL").executeQuery()
        resultSet.next()
        String version = resultSet.getString(1)

        then:
        version == '1.4.196'

        cleanup:
        applicationContext.close()
    }

    @Ignore // fails intermittently. TODO: investigate
    void "test properties are bindable"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.connectionTimeout': 500,
                'datasources.default.idleTimeout': 20000,
                'datasources.default.catalog': 'foo',
                'datasources.default.autoCommit': true,
                'datasources.default.healthCheckProperties.foo': 'bar',
                'datasources.default.jndiName': 'java:comp/env/FooBarPool',
                'datasources.default.url': 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.validationQuery': 'select 3']
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(HikariDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        HikariDataSource dataSource = applicationContext.getBean(HikariDataSource)

        then:
        dataSource.connectionTimeout == 500
        dataSource.idleTimeout == 20000
        dataSource.catalog == 'foo'
        dataSource.autoCommit
        dataSource.healthCheckProperties.getProperty('foo') == 'bar'
        dataSource.dataSourceJNDI == 'java:comp/env/FooBarPool'
        dataSource.jdbcUrl == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.connectionTestQuery == 'select 3'

        cleanup:
        applicationContext.close()
    }

    void "test multiple data sources are configured"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                'datasources.foo': [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(HikariDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        HikariDataSource dataSource = applicationContext.getBean(HikariDataSource)

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.jdbcUrl == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        when:
        dataSource = applicationContext.getBean(HikariDataSource, Qualifiers.byName("foo"))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.jdbcUrl == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }
}