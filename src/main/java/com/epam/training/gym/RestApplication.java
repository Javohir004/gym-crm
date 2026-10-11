package com.epam.training.gym;

import com.epam.training.gym.config.AppConfig;
import com.epam.training.gym.config.WebConfig;
import com.epam.training.gym.logging.RestCallLoggingFilter;
import com.epam.training.gym.logging.TransactionIdFilter;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;
import java.nio.file.Files;


public class RestApplication {

    private static final Logger log = LoggerFactory.getLogger(RestApplication.class);
    private static final int PORT = 8081;

    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector();

        File baseDir = createTempBaseDir();
        tomcat.setBaseDir(baseDir.getAbsolutePath());

        Context context = tomcat.addContext("", baseDir.getAbsolutePath());

        addFilter(context, "transactionIdFilter", TransactionIdFilter.class);
        addFilter(context, "restCallLoggingFilter", RestCallLoggingFilter.class);

        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        webContext.register(AppConfig.class, WebConfig.class);

        DispatcherServlet dispatcherServlet = new DispatcherServlet(webContext);
        Tomcat.addServlet(context, "dispatcher", dispatcherServlet).setLoadOnStartup(1);
        context.addServletMappingDecoded("/*", "dispatcher");

        tomcat.start();
        log.info("Gym CRM REST API started on http://localhost:{}", PORT);
        tomcat.getServer().await();
    }

    private static void addFilter(Context context, String name, Class<? extends jakarta.servlet.Filter> filterClass) {
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName(name);
        filterDef.setFilterClass(filterClass.getName());
        context.addFilterDef(filterDef);

        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(name);
        filterMap.addURLPattern("/*");
        context.addFilterMap(filterMap);
    }

    private static File createTempBaseDir() {
        try {
            return Files.createTempDirectory("gym-crm-tomcat").toFile();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create a temp directory for embedded Tomcat", e);
        }
    }
}