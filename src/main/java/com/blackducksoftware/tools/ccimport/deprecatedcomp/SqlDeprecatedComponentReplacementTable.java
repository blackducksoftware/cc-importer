package com.blackducksoftware.tools.ccimport.deprecatedcomp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationPassword;
import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;

/**
 * Load deprecated component replacement via SQL.
 * As of the Cactus release, we should be able to do this via the SDK, and we can replace this implementation.
 *
 * @author sbillings
 *
 */
public class SqlDeprecatedComponentReplacementTable implements DeprecatedComponentReplacementTable {
    private static final String SQL_GET_VERSIONED = "SELECT old_project_id, old_release_id, new_project_id, new_release_id FROM standard_project_release_deprecated";

    private static final String SQL_GET_UNVERSIONED = "SELECT old_project_id, new_project_id FROM standard_project_deprecated";

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private final Map<ComponentNameVersionIds, ComponentNameVersionIds> table = new HashMap<>(512);

    public SqlDeprecatedComponentReplacementTable(final ConfigurationManager config) throws CodeCenterImportException {
        final String dbServerName = config.getProperty("protex.db.server");
        String dbUserName = config.getOptionalProperty("protex.db.user.name");
        if (dbUserName == null) {
            dbUserName = "blackduck";
        }

        // Read the value of protex.db.password
        String dbPassword = "mallard";
        final ConfigurationPassword configurationPassword = ConfigurationPassword.createFromProperty(config.getProps(), "protex.db");
        // test to see if the property was set or not
        if (configurationPassword.getPlainText() != null) {
            // get the plain text value of the password (even if it was encrypted in the property file)
            dbPassword = configurationPassword.getPlainText();
        }
        String dbPort = config.getOptionalProperty("protex.db.port");
        if (dbPort == null) {
            dbPort = "55432";
        }
        String dbName = config.getOptionalProperty("protex.db.replacements.dbname");
        if (dbName == null) {
            dbName = "bds_customer";
        }
        log.info("Reading database " + dbName + " for replacement tables (standard_project_release_deprecated, standard_project_deprecated)");
        try {
            final Connection conn = connectToDb(dbServerName, dbPort, dbName, dbUserName, dbPassword);
            loadVersioned(conn, table);
            loadUnVersioned(conn, table);
            conn.close();
        } catch (final SQLException e) {
            final String msg = "Error loading replacement table from database " + dbName + " on " + dbServerName + ": " + e.getMessage();
            log.error(msg);
            throw new CodeCenterImportException(msg);
        }
    }

    private void loadVersioned(final Connection conn, final Map<ComponentNameVersionIds, ComponentNameVersionIds> table) throws SQLException {
        final Statement stmt = conn.createStatement();
        final ResultSet rs = stmt.executeQuery(SQL_GET_VERSIONED);

        while (rs.next()) {
            final String oldCompId = rs.getString("old_project_id");
            final String oldCompVersionId = rs.getString("old_release_id");
            final String newCompId = rs.getString("new_project_id");
            final String newCompVersionId = rs.getString("new_release_id");
            log.debug("Old Comp ID: " + oldCompId + ", OldCompVersionId: " + oldCompVersionId + ", New Comp ID: " + newCompId + ", New Comp Version ID: "
                    + newCompVersionId);
            addToTable(table, oldCompId, oldCompVersionId, newCompId, newCompVersionId);
        }
    }

    private void loadUnVersioned(final Connection conn, final Map<ComponentNameVersionIds, ComponentNameVersionIds> table) throws SQLException {
        final Statement stmt = conn.createStatement();
        final ResultSet rs = stmt.executeQuery(SQL_GET_UNVERSIONED);

        while (rs.next()) {
            final String oldCompId = rs.getString("old_project_id");
            final String newCompId = rs.getString("new_project_id");
            log.debug("Old Comp ID: " + oldCompId + ", New Comp ID: " + newCompId);

            addToTable(table, oldCompId, null, newCompId, null);
        }
    }

    private void addToTable(final Map<ComponentNameVersionIds, ComponentNameVersionIds> table, final String oldCompId, final String oldCompVersionId,
            final String newCompId,
            final String newCompVersionId) {
        final ComponentNameVersionIds deprecatedComponent = new ComponentNameVersionIds(oldCompId, oldCompVersionId);
        final ComponentNameVersionIds replacementComponent = new ComponentNameVersionIds(newCompId, newCompVersionId);
        table.put(deprecatedComponent, replacementComponent);
    }

    @Override
    public ComponentNameVersionIds getReplacement(final ComponentNameVersionIds deprecatedComponent) {
        return table.get(deprecatedComponent);
    }

    private Connection connectToDb(final String dbServerName, final String dbPort, final String dbName, final String dbUserName, final String dbPassword)
            throws SQLException {
        final String url = "jdbc:postgresql://" + dbServerName + ":" + dbPort + "/" + dbName;
        final Properties dbProps = new Properties();
        dbProps.setProperty("user", dbUserName);
        dbProps.setProperty("password", dbPassword);
        log.info("Connecting to: " + url);
        return DriverManager.getConnection(url, dbProps);
    }

    @Override
    public Set<ComponentNameVersionIds> getDeprecatedComponents() {
        return table.keySet();
    }
}
