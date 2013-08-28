package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.chunk.CustomMultiChunkerTest;
import org.syncany.tests.chunk.FixedOffsetChunkerTest;
import org.syncany.tests.chunk.FrameworkCombinationTest;
import org.syncany.tests.chunk.TTTDChunkerTest;
import org.syncany.tests.config.EncryptionTest;
import org.syncany.tests.connection.plugins.local.LocalConnectionPluginTest;
import org.syncany.tests.database.DatabaseWriteReadIndividualObjectsTest;
import org.syncany.tests.database.VectorClockTest;
import org.syncany.tests.operations.IndexerTest;
import org.syncany.tests.operations.SyncUpOperationTest;
import org.syncany.tests.scenarios.EvilCScenarioTest;

@RunWith(Suite.class)
@SuiteClasses({
	// Config	
	EncryptionTest.class,
	
	// Chunking Framework
	CustomMultiChunkerTest.class,
	FixedOffsetChunkerTest.class,
	TTTDChunkerTest.class,
	FrameworkCombinationTest.class,
	
	// Connection
	LocalConnectionPluginTest.class,
	
	// Database
	DatabaseWriteReadIndividualObjectsTest.class,
	VectorClockTest.class,
	
	// Operations
	IndexerTest.class,
	SyncUpOperationTest.class,
	
	// Scenarios
	EvilCScenarioTest.class
})
public class AllTestsSuite {
	// This class executes all tests
}
