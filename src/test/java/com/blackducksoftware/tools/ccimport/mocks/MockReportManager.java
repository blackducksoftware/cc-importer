package com.blackducksoftware.tools.ccimport.mocks;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.protex.report.Format;
import com.blackducksoftware.tools.connector.protex.report.IReportManager;
import com.blackducksoftware.tools.connector.protex.report.ReportPojo;
import com.blackducksoftware.tools.connector.protex.report.ReportSectionSelection;

public class MockReportManager implements IReportManager {
    private static final String OBLIGATIONS_CSV = "src/test/resources/savedreports/csv/obligations.csv";

    @Override
    public ReportPojo generateAdHocProjectReportSingleSection(String projectId,
            ReportSectionSelection section, String name, String sectionTitle,
            Format format, boolean includeTableOfContents)
            throws CommonFrameworkException {

        return mockTheReportBySection(OBLIGATIONS_CSV);
    }

    /**
     * The mock here is the actual data source, naming is not relevant.
     * 
     * @param sectionFile
     * @return
     */
    private ReportPojo mockTheReportBySection(String sectionFile) {

        DataSource dataSource = new FileDataSource(sectionFile);
        DataHandler dataHandler = new DataHandler(dataSource);
        ReportPojo report = new ReportPojo(dataHandler,
                getFullPathOfLocalFile(sectionFile));

        return report;
    }

    /**
     * Return the absolute path of the file
     * 
     * @param fileName
     *            the file name
     * @return the absolute path of the file
     */
    private String getFullPathOfLocalFile(String fileName) {
        File file = new File(fileName);
        return file.getAbsolutePath();
    }
}
