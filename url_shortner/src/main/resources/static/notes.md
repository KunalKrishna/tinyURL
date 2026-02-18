# Customizing ErrorPage 

*Disable Whitelabel Error Page*

application.properties 
-  `server.error.whitelabel.enabled=false`
-  Or, `spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration`
-  Or `@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})`

Better Add Custom View
- Or, Controller-View Technique
-----------------------------
STATIC Pages
-----------------------------
you can place the views like `error.html` in "`res/static`" w/o importing thymeleaf dependency
but you will need to return return "`error.html`"
```java
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        //do something like logging
        return "error.html";
    }
}
```
**Limitations** : Cannot map dynamic data(Model) on views.

-----------------------------
DYNAMIC Pages(using Thymeleaf)
-----------------------------
place views like error.html in "res/resources/templates" & add Thymleaf in pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```
and write a controller returning view name
```java
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        model.setAttribute("key", "value");
        model.setAttribute("key", "value");
        model.setAttribute("key", "value");
        return "error";
    }
}
```
and view the attribute using `th:text="${title}"`
```html
<!DOCTYPE html>
<html lang="en"
    xmlns:th="https://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Home</title>
</head>
<body>
<h1 th:text="${title}"> Project Name</h1>
</body>
</html>
```

NOTE: @RequestMapping is a generic request mapper which accepts all type of HTTP verbs. 
To be more specific use its special subtypes e.g. GetMapping, PostMapping etc.

#### Specialized Composed Annotations
Since Spring 4.3, method-level variants (composed annotations) were introduced as a shortcut and best practice for improved readability. These are meta-annotated with @RequestMapping.
- `@GetMapping` (shortcut for `@RequestMapping(method = RequestMethod.GET)`)
- `@PostMappin`g (shortcut for `@RequestMapping(method = RequestMethod.POST)`)
- `@PutMapping` (shortcut for `@RequestMapping(method = RequestMethod.PUT)`)
- `@DeleteMapping` (shortcut for `@RequestMapping(method = RequestMethod.DELETE)`)
- `@PatchMapping` (shortcut for `@RequestMapping(method = RequestMethod.PATCH)`)

Now, we have made the view a dynamic page. 
Now, instead of returning views as html what if method returns JSON data so that we can 
facilitate machine to machine communication using APIs(Rest API to be specific) ?

Because
`return "{ \"message\" : \"Welcome to URL Shortner\"}";` 
gives following error
`org.thymeleaf.exceptions.TemplateInputException: Error resolving template [{ "message" : "Welcome to URL Shortner"}],`

**Solution** : use `@ResponseBody` annotation on method

```java
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public String handleError(HttpServletRequest request) {
        return "{ \"message\" : \"Welcome to URL Shortner\"}";
    }
}
```

Still further if all methods of the controller is going to return JSON better to delegate
`@ResponseBody` at class level.
```java
@ResponseBody
@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request) {
        return "{ \"message\" : \"Welcome to URL Shortner\"}";
    }
    
    @GetMapping("/about")
    public String about(HttpServletRequest request) {
        return "{ \"about\" : \"Description of website\"}";
    }
}
```
Still combine two `@ResponseBody` + `@Controller` $$\rightarrow$$ `@RestController` 

#### Enabling Spring Boot DevTools in IntelliJ IDEA
https://youtu.be/kIAtwEcb6JI?si=jWsZQcxXQt-z2pM2


### JPA (Java/ Jakarta Persistence API )
:= a standardized Java specification for managing relational data in Java applications, allowing developers to 
map Java objects (POJOs) to database tables using Object-Relational Mapping (ORM). It facilitates database operations without writing complex SQL.

```
@Entity
@Table(name="table_name")
POJO          ===JPA===>       TABLE
(Java         using ORM      (Database)
Class)
```
- **Key Purpose**: Simplifies data persistence (saving/loading objects) in Java SE and Java EE/Jakarta EE environments.
- **Implementations**: JPA is a specification, not a tool, and is implemented by frameworks like Hibernate, EclipseLink, and TopLink.
- **Components**: Key components include Entity Managers, Persistence Context, and JPQL (Java Persistence Query Language).
- **Advantage**: Reduces boilerplate code and increases portability across different databases.

https://www.infoworld.com/article/2259807/what-is-jpa-introduction-to-the-java-persistence-api.html
> Another essential piece of the Java persistence puzzle, JDBC (the Java Database Connectivity API). is the standard mechanism for directly accessing a database in Java using SQL statements. The core difference between the JPA and JDBC specifications is that, for the most part, JPA lets you avoid the need to “think relationally”—meaning you never have to deal directly with SQL. JPA lets you define your persistence rules using Java code and objects, whereas JDBC requires you to manually translate from code to relational tables and back again.



Table creation + Data population (migration) : Three evolutionary ways : 
1. JDBC only
2. Hibernate + JPA

JDBC only
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
JPA over JDBC for concise code. Read more : https://www.baeldung.com/spring-boot-data-sql-and-schema-sql 
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

now simply add `@Entity` `@Table` to directly interact with table/DB.
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgresDB
spring.datasource.username=root
spring.datasource.password=root

spring.jpa.hibernate.ddl-auto=create 
```

But if we need more fine-grained control over the database alterations use `data.sql` & `schema.sql` in `resources.`
And add following properties while disabling hibernate DDL property.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgresDB
spring.datasource.username=root
spring.datasource.password=root

#spring.jpa.hibernate.ddl-auto=[create/ update ...]

spring.sql.init.mode=always
#Ensures initialization runs, even for non-embedded databases
```

----------------
From JPA to FlyWay Migration
-------------------------

Case I : No properties set in `application.properties`
Result : no table added in DB.
Log : 
```log
2026-02-18T00:09:17.687-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-02-18T00:09:17.885-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@308c55ee
2026-02-18T00:09:17.888-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-02-18T00:09:18.011-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-02-18T00:09:18.060-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.6.41.Final
2026-02-18T00:09:18.094-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-02-18T00:09:18.360-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-02-18T00:09:18.450-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [Connecting through datasource 'HikariDataSource (HikariPool-1)']
	Database driver: undefined/unknown
	Database version: 17.8
	Autocommit mode: undefined/unknown
	Isolation level: undefined/unknown
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-02-18T00:09:19.500-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-02-18T00:09:19.502-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-02-18T00:09:19.547-05:00  WARN 42524 --- [spring-boot-url-shortner] [  restartedMain] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-02-18T00:09:19.571-05:00  INFO 42524 --- [spring-boot-url-shortner] [  restartedMain] o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page template: index
```

Case II : after adding `spring.jpa.hibernate.ddl-auto=create`
Result : All Tables annotated with `@Entity` added in DB.
Log :
```log
same as above
```

Case III : add rows in `data.sql` and property `spring.sql.init.mode=always` in application.properties
```properties
spring.jpa.hibernate.ddl-auto=create
spring.sql.init.mode=always
```
```sql
INTO short_urls (short_key, original_url, created_by, created_at, expires_at, is_private, click_count)
VALUES ('rs1Aed', 'https://www.sivalabs.in/code-offline-with-local-ai-ollama', 1, TIMESTAMP '2024-07-15', NULL, FALSE,
        0),
```
Result : No table created
```log
2026-02-18T00:17:03.200-05:00  INFO 25848 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-02-18T00:17:03.345-05:00  INFO 25848 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@777fd96e
2026-02-18T00:17:03.346-05:00  INFO 25848 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-02-18T00:17:03.392-05:00  WARN 25848 --- [spring-boot-url-shortner] [  restartedMain] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: Failed to initialize dependency 'dataSourceScriptDatabaseInitializer' of LoadTimeWeaverAware bean 'entityManagerFactory': Error creating bean with name 'dataSourceScriptDatabaseInitializer' defined in class path resource [org/springframework/boot/autoconfigure/sql/init/DataSourceInitializationConfiguration.class]: Failed to execute SQL script statement #1 of file [E:\SpringProjects\tinyURL\url_shortner\target\classes\data.sql]: INSERT INTO users (email, password, name, role, created_at) VALUES ('admin@gmail.com', 'admin', 'Administrator', 'ROLE_ADMIN', TIMESTAMP '2024-07-15'), ('siva@gmail.com', 'secret', 'Siva', 'ROLE_USER', TIMESTAMP '2024-07-15')
2026-02-18T00:17:03.392-05:00  INFO 25848 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2026-02-18T00:17:03.394-05:00  INFO 25848 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
2026-02-18T00:17:03.400-05:00  INFO 25848 --- [spring-boot-url-shortner] [  restartedMain] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2026-02-18T00:17:03.423-05:00  INFO 25848 --- [spring-boot-url-shortner] [  restartedMain] .s.b.a.l.ConditionEvaluationReportLogger : 

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-02-18T00:17:03.456-05:00 ERROR 25848 --- [spring-boot-url-shortner] [  restartedMain] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: Failed to initialize dependency 'dataSourceScriptDatabaseInitializer' of LoadTimeWeaverAware bean 'entityManagerFactory': Error creating bean with name 'dataSourceScriptDatabaseInitializer' defined in class path resource [org/springframework/boot/autoconfigure/sql/init/DataSourceInitializationConfiguration.class]: Failed to execute SQL script statement #1 of file [E:\SpringProjects\tinyURL\url_shortner\target\classes\data.sql]: INSERT INTO users (email, password, name, role, created_at) VALUES ('admin@gmail.com', 'admin', 'Administrator', 'ROLE_ADMIN', TIMESTAMP '2024-07-15'), ('siva@gmail.com', 'secret', 'Siva', 'ROLE_USER', TIMESTAMP '2024-07-15')
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:328) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:207) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:973) ~[spring-context-6.2.15.jar:6.2.15]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:627) ~[spring-context-6.2.15.jar:6.2.15]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:439) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:318) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1361) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1350) ~[spring-boot-3.5.10.jar:3.5.10]
	at com.abitmanipulator.url_shortner.UrlShortnerApp.main(UrlShortnerApp.java:13) ~[classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
	at org.springframework.boot.devtools.restart.RestartLauncher.run(RestartLauncher.java:50) ~[spring-boot-devtools-3.5.10.jar:3.5.10]
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'dataSourceScriptDatabaseInitializer' defined in class path resource [org/springframework/boot/autoconfigure/sql/init/DataSourceInitializationConfiguration.class]: Failed to execute SQL script statement #1 of file [E:\SpringProjects\tinyURL\url_shortner\target\classes\data.sql]: INSERT INTO users (email, password, name, role, created_at) VALUES ('admin@gmail.com', 'admin', 'Administrator', 'ROLE_ADMIN', TIMESTAMP '2024-07-15'), ('siva@gmail.com', 'secret', 'Siva', 'ROLE_USER', TIMESTAMP '2024-07-15')
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1826) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:607) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:529) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:339) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:373) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:337) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:202) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:315) ~[spring-beans-6.2.15.jar:6.2.15]
	... 13 common frames omitted
Caused by: org.springframework.jdbc.datasource.init.ScriptStatementFailedException: Failed to execute SQL script statement #1 of file [E:\SpringProjects\tinyURL\url_shortner\target\classes\data.sql]: INSERT INTO users (email, password, name, role, created_at) VALUES ('admin@gmail.com', 'admin', 'Administrator', 'ROLE_ADMIN', TIMESTAMP '2024-07-15'), ('siva@gmail.com', 'secret', 'Siva', 'ROLE_USER', TIMESTAMP '2024-07-15')
	at org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(ScriptUtils.java:293) ~[spring-jdbc-6.2.15.jar:6.2.15]
	at org.springframework.jdbc.datasource.init.ResourceDatabasePopulator.populate(ResourceDatabasePopulator.java:254) ~[spring-jdbc-6.2.15.jar:6.2.15]
	at org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute(DatabasePopulatorUtils.java:54) ~[spring-jdbc-6.2.15.jar:6.2.15]
	at org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer.runScripts(DataSourceScriptDatabaseInitializer.java:87) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer.runScripts(AbstractScriptDatabaseInitializer.java:146) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer.applyScripts(AbstractScriptDatabaseInitializer.java:108) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer.applyDataScripts(AbstractScriptDatabaseInitializer.java:102) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer.initializeDatabase(AbstractScriptDatabaseInitializer.java:77) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer.afterPropertiesSet(AbstractScriptDatabaseInitializer.java:66) ~[spring-boot-3.5.10.jar:3.5.10]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1873) ~[spring-beans-6.2.15.jar:6.2.15]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1822) ~[spring-beans-6.2.15.jar:6.2.15]
	... 20 common frames omitted
Caused by: org.postgresql.util.PSQLException: ERROR: relation "users" does not exist
  Position: 13
	at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2846) ~[postgresql-42.7.9.jar:42.7.9]
	at org.postgresql.core.v3.QueryExecutorImpl.processResults(QueryExecutorImpl.java:2531) ~[postgresql-42.7.9.jar:42.7.9]
	at org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:429) ~[postgresql-42.7.9.jar:42.7.9]
	at org.postgresql.jdbc.PgStatement.executeInternal(PgStatement.java:526) ~[postgresql-42.7.9.jar:42.7.9]
	at org.postgresql.jdbc.PgStatement.execute(PgStatement.java:436) ~[postgresql-42.7.9.jar:42.7.9]
	at org.postgresql.jdbc.PgStatement.executeWithFlags(PgStatement.java:358) ~[postgresql-42.7.9.jar:42.7.9]
	at org.postgresql.jdbc.PgStatement.executeCachedSql(PgStatement.java:343) ~[postgresql-42.7.9.jar:42.7.9]
	at org.postgresql.jdbc.PgStatement.executeWithFlags(PgStatement.java:319) ~[postgresql-42.7.9.jar:42.7.9]
	at org.postgresql.jdbc.PgStatement.execute(PgStatement.java:314) ~[postgresql-42.7.9.jar:42.7.9]
	at com.zaxxer.hikari.pool.ProxyStatement.execute(ProxyStatement.java:95) ~[HikariCP-6.3.3.jar:na]
	at com.zaxxer.hikari.pool.HikariProxyStatement.execute(HikariProxyStatement.java) ~[HikariCP-6.3.3.jar:na]
	at org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(ScriptUtils.java:261) ~[spring-jdbc-6.2.15.jar:6.2.15]
	... 30 common frames omitted


Process finished with exit code 0

```
Takeaway : No table created because data population assumes DDL already done and proceeds DDL.  Fix ? Next case

Case IV : Add `spring.jpa.defer-datasource-initialization=true`
```properties
# By default, data.sql scripts get executed before the Hibernate is initialized.
# We need Hibernate to create our tables before inserting the data into them. To achieve this,
# we need to defer the initialization of our data source. We’ll use the below property to achieve this:
spring.jpa.defer-datasource-initialization=true
spring.jpa.hibernate.ddl-auto=create
spring.sql.init.mode=always
```
Result : All tables created & all tables initialized. 
Takeaway: `spring.jpa.defer-datasource-initialization=true` ensures Schema Generation(using Hibernate) precedes Data Initialization(Population)

Case V : Define both schema generation (schema.sql) and  data population(data.sql) in properties.
```properties
# just single line enough:
spring.sql.init.mode=always
```
However, if we re-run spring boot application it gives error
```log
Caused by: org.postgresql.util.PSQLException: ERROR: relation "users" already exists
	at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2846) ~[postgresql-42.7.9.jar:42.7.9]
```
because it reruns sql scripts and try creating tables again and inserting data.
How to avoid? **Flyway Migrations**
- import dependency through pom.xml
- resources/db/migration -> places scripts with name format V1__name.sql
- remove/comment : `spring.sql.init.mode=always`

Result : 
```log
2026-02-18T04:14:11.315-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-02-18T04:14:11.351-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://localhost:5432/postgresDB (PostgreSQL 17.8)
2026-02-18T04:14:11.410-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.f.c.i.s.JdbcTableSchemaHistory         : Schema history table "public"."flyway_schema_history" does not exist yet
2026-02-18T04:14:11.418-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.f.core.internal.command.DbValidate     : Successfully validated 2 migrations (execution time 00:00.024s)
2026-02-18T04:14:11.449-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.f.c.i.s.JdbcTableSchemaHistory         : Creating Schema History table "public"."flyway_schema_history" ...
2026-02-18T04:14:11.631-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.f.core.internal.command.DbMigrate      : Current version of schema "public": << Empty Schema >>
2026-02-18T04:14:11.652-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "1 - create tables"
2026-02-18T04:14:11.759-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "2 - insert sample data"
2026-02-18T04:14:11.828-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.f.core.internal.command.DbMigrate      : Successfully applied 2 migrations to schema "public", now at version v2 (execution time 00:00.087s)
2026-02-18T04:14:12.011-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-02-18T04:14:12.147-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.6.41.Final
2026-02-18T04:14:12.223-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-02-18T04:14:12.675-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-02-18T04:14:12.826-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
	Database JDBC URL [Connecting through datasource 'HikariDataSource (HikariPool-1)']
	Database driver: undefined/unknown
	Database version: 17.8
	Autocommit mode: undefined/unknown
	Isolation level: undefined/unknown
	Minimum pool size: undefined/unknown
	Maximum pool size: undefined/unknown
2026-02-18T04:14:14.355-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-02-18T04:14:14.359-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-02-18T04:14:14.405-05:00  WARN 27256 --- [spring-boot-url-shortner] [  restartedMain] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2026-02-18T04:14:14.445-05:00  INFO 27256 --- [spring-boot-url-shortner] [  restartedMain] o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page template: index
```