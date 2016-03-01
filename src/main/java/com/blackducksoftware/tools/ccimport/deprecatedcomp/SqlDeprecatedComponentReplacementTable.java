package com.blackducksoftware.tools.ccimport.deprecatedcomp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;

public class SqlDeprecatedComponentReplacementTable implements DeprecatedComponentReplacementTable {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Map<ComponentNameVersionIds, ComponentNameVersionIds> table;

    public SqlDeprecatedComponentReplacementTable(ConfigurationManager config) throws CodeCenterImportException {
        String dbServerName = config.getProperty("protex.db.server");
        String dbUserName = config.getOptionalProperty("protex.db.user.name");
        if (dbUserName == null) {
            dbUserName = "blackduck";
        }
        String dbPassword = config.getOptionalProperty("protex.db.password");
        if (dbPassword == null) {
            dbPassword = "mallard";
        }
        String dbPort = config.getOptionalProperty("protex.db.port");
        if (dbPort == null) {
            dbPort = "55432";
        }
        String dbName = config.getOptionalProperty("protex.db.replacements.dbname");
        if (dbName == null) {
            dbName = "bds_customer";
        }
        try {
            Connection conn = connectToDb(dbServerName, dbPort, dbName, dbUserName, dbPassword);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT old_project_id, old_release_id, new_project_id, new_release_id FROM standard_project_release_deprecated");
            while (rs.next()) {
                String oldCompId = rs.getString("old_project_id");
                String oldCompVersionId = rs.getString("old_release_id");
                String newCompId = rs.getString("new_project_id");
                String newCompVersionId = rs.getString("new_release_id");
                ComponentNameVersionIds deprecatedComponent = new ComponentNameVersionIds(oldCompId, oldCompVersionId);
                ComponentNameVersionIds replacementComponent = new ComponentNameVersionIds(newCompId, newCompVersionId);
                table.put(deprecatedComponent, replacementComponent);
            }
            conn.close();
        } catch (SQLException e) {
            String msg = "Error loading replacement table from database " + dbName + " on " + dbServerName + ": " + e.getMessage();
            log.error(msg);
            throw new CodeCenterImportException(msg);
        }
    }

    @Override
    public ComponentNameVersionIds getReplacement(ComponentNameVersionIds deprecatedComponent) {
        return table.get(deprecatedComponent);
    }

    private Connection connectToDb(String dbServerName, String dbPort, String dbName, String dbUserName, String dbPassword)
            throws SQLException {
        String url = "jdbc:postgresql://" + dbServerName + ":" + dbPort + "/" + dbName;
        Properties dbProps = new Properties();
        dbProps.setProperty("user", dbUserName);
        dbProps.setProperty("password", dbPassword);
        log.info("Connecting to: " + url);
        return DriverManager.getConnection(url, dbProps);
    }
}
