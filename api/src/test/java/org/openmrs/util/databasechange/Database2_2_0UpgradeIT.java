/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.util.databasechange;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.util.DatabaseUtil;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * Tests database upgrade from OpenMRS 2.2.0.
 */
public class Database2_2_0UpgradeIT extends BaseContextSensitiveTest {
	
	public static final String TEST_DATA_DIR = "/org/openmrs/util/databasechange/";
	
	public static final String UPGRADE_TEST_2_1_0_TO_2_20_DATASET = TEST_DATA_DIR
		+ "database2_1To2_20UpgradeTest-dataSet.xml";
	
	public static final String STANDARD_TEST_2_2_0_DATASET = TEST_DATA_DIR + "standardTest-2.1.9-dataSet.xml";
	
	public final static String DATABASE_PATH = TEST_DATA_DIR + "openmrs-2.1.9.h2.db";
	
	private DatabaseUpgradeTestUtil upgradeTestUtil;
	
	private static File testAppDataDir;
	
	@BeforeClass
	public static void beforeClass() throws IOException {
		testAppDataDir = File.createTempFile("appdir-for-unit-tests", "");
		testAppDataDir.delete();// so we can make turn it into a directory
		testAppDataDir.mkdir();
		
		System.setProperty(OpenmrsConstants.APPLICATION_DATA_DIRECTORY_RUNTIME_PROPERTY, testAppDataDir.getAbsolutePath());
		OpenmrsUtil.setApplicationDataDirectory(testAppDataDir.getAbsolutePath());
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		FileUtils.deleteDirectory(testAppDataDir);
		//Just to be safe, not to affect other units in the test suite
		System.clearProperty(OpenmrsConstants.APPLICATION_DATA_DIRECTORY_RUNTIME_PROPERTY);
	}
	
	@Before
	public void before() throws IOException, SQLException {
		upgradeTestUtil = new DatabaseUpgradeTestUtil(DATABASE_PATH);
	}
	
	@After
	public void after() throws SQLException {
		upgradeTestUtil.close();
	}
	
	/**
	 * This method creates mock order entry upgrade file
	 *
	 * @see org.openmrs.util.UpgradeUtil#getConceptIdForUnits(String)
	 */
	public static void createOrderEntryUpgradeFileWithTestData(String propString) throws IOException {
		Properties props = new Properties();
		props.load(new StringReader(propString));
		String appDataDir = OpenmrsUtil.getApplicationDataDirectory();
		File propFile = new File(appDataDir, DatabaseUtil.ORDER_ENTRY_UPGRADE_SETTINGS_FILENAME);
		props.store(new FileWriter(propFile), null);
		propFile.deleteOnExit();
	}
	
	@Test
	public void shouldHaveUpdatedOrderTableInDatabase() throws Exception {
		String FULFILLER_STATUS = "RECEIVED";
		String FULFILLER_COMMENT = "Comment";
		
		upgradeTestUtil.executeDataset(STANDARD_TEST_2_2_0_DATASET);
		upgradeTestUtil.executeDataset(UPGRADE_TEST_2_1_0_TO_2_20_DATASET);
		createOrderEntryUpgradeFileWithTestData("mg=111\ntab(s)=112\n1/day\\ x\\ 7\\ days/week=113\n2/day\\ x\\ 7\\ days/week=114");
		
		upgradeTestUtil.upgrade();
		Connection connection = upgradeTestUtil.getConnection();
		final String insertOrderQuery = "UPDATE orders SET fulfiller_status='"+ FULFILLER_STATUS + "' , fulfiller_comment = '" +FULFILLER_COMMENT + "' where order_id = 6 ";
		DatabaseUtil.executeSQL(connection, insertOrderQuery, false);
		connection.commit();
		List<Map<String, String>> orders = upgradeTestUtil.select("orders", "fulfiller_comment = 'Comment' and fulfiller_status='RECEIVED'",
			"fulfiller_status", "fulfiller_comment");
		
		assertTrue(orders.size() == 1);
		
	}
	
}
