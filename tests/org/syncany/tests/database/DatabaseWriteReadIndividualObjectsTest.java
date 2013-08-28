package org.syncany.tests.database;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseXmlDAO;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.operations.RemoteDatabaseFile;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestFileUtil;

public class DatabaseWriteReadIndividualObjectsTest {
	private File tempDir;
	Random generator;
	
	@Before
	public void setUp() throws Exception {
		tempDir = TestFileUtil.createTempDirectoryInSystemTemp();		
		generator = new Random();
	}
	
	@After
	public void tearDown() {
		//TestFileUtil.deleteDirectory(tempDir);
	}
	 
	@Test
	public void testWriteAndReadChunks() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
		
		// Create chunks
        ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0}, 12);
        ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,8,7,6,5,4,3,2,1}, 34);
        ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 1,1,1,1,1,1,1,1,1}, 56);
        ChunkEntry chunkA4 = new ChunkEntry(new byte[] { 2,2,2,2,2,2,2,2,2}, 78);
        
        newDatabaseVersion.addChunk(chunkA1);
        newDatabaseVersion.addChunk(chunkA2);
        newDatabaseVersion.addChunk(chunkA3);
        newDatabaseVersion.addChunk(chunkA4);
		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		
		// Check chunks
		assertEquals("Chunk not found in database loaded.", chunkA1, loadedDatabase.getChunk(chunkA1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA2, loadedDatabase.getChunk(chunkA2.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA3, loadedDatabase.getChunk(chunkA3.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA4, loadedDatabase.getChunk(chunkA4.getChecksum()));
	}
		
	@Test
	public void testWriteAndReadChunksWithMultiChunks() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
		
		// Create chunks
        ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0}, 12);
        ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,8,7,6,5,4,3,2,1}, 34);
        ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 1,1,1,1,1,1,1,1,1}, 56);
        ChunkEntry chunkA4 = new ChunkEntry(new byte[] { 2,2,2,2,2,2,2,2,2}, 78);

        ChunkEntry chunkB1 = new ChunkEntry(new byte[] { 3,3,3,3,3,3,3,3,3}, 910);
        ChunkEntry chunkB2 = new ChunkEntry(new byte[] { 4,4,4,4,4,4,4,4,4}, 1112);
        
        newDatabaseVersion.addChunk(chunkA1);
        newDatabaseVersion.addChunk(chunkA2);
        newDatabaseVersion.addChunk(chunkA3);
        newDatabaseVersion.addChunk(chunkA4);
        newDatabaseVersion.addChunk(chunkB1);
        newDatabaseVersion.addChunk(chunkB2);        
        
        // Distribute chunks to multichunks
        MultiChunkEntry multiChunkA = new MultiChunkEntry(new byte[] {6,6,6,6,6,6,6,6,6});
        multiChunkA.addChunk(new ChunkEntryId(chunkA1.getChecksum())); 
        multiChunkA.addChunk(new ChunkEntryId(chunkA2.getChecksum())); 
        multiChunkA.addChunk(new ChunkEntryId(chunkA3.getChecksum()));
        newDatabaseVersion.addMultiChunk(multiChunkA);
        
        MultiChunkEntry multiChunkB = new MultiChunkEntry(new byte[] {7,7,7,7,7,7,7,7,7});
        multiChunkB.addChunk(new ChunkEntryId(chunkA4.getChecksum()));
        multiChunkB.addChunk(new ChunkEntryId(chunkB1.getChecksum()));
        multiChunkB.addChunk(new ChunkEntryId(chunkB2.getChecksum()));
        newDatabaseVersion.addMultiChunk(multiChunkB);        
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		
		// Check chunks
		assertEquals("Chunk not found in database loaded.", chunkA1, loadedDatabase.getChunk(chunkA1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA2, loadedDatabase.getChunk(chunkA2.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA3, loadedDatabase.getChunk(chunkA3.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA4, loadedDatabase.getChunk(chunkA4.getChecksum()));

		assertEquals("Chunk not found in database loaded.", chunkB1, loadedDatabase.getChunk(chunkB1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkB2, loadedDatabase.getChunk(chunkB2.getChecksum()));

		// Check multichunks
		MultiChunkEntry loadedMultiChunkA = loadedDatabase.getMultiChunk(multiChunkA.getId());
		MultiChunkEntry loadedMultiChunkB = loadedDatabase.getMultiChunk(multiChunkB.getId());
		
		assertEquals("Multichunk not found in database loaded.", multiChunkA, loadedMultiChunkA);
		assertEquals("Multichunk not found in database loaded.", multiChunkB, loadedMultiChunkB);
	
		assertArrayEquals("Chunks in multichunk expected to be different.", multiChunkA.getChunks().toArray(), loadedMultiChunkA.getChunks().toArray());
		assertArrayEquals("Chunks in multichunk expected to be different.", multiChunkB.getChunks().toArray(), loadedMultiChunkB.getChunks().toArray());
	}	
	
	@Test
	public void testWriteAndReadChunksWithFileContents() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
		
		// Create chunks
        ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0}, 12);
        ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,8,7,6,5,4,3,2,1}, 34);
        ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 1,1,1,1,1,1,1,1,1}, 56);
        ChunkEntry chunkA4 = new ChunkEntry(new byte[] { 2,2,2,2,2,2,2,2,2}, 78);

        ChunkEntry chunkB1 = new ChunkEntry(new byte[] { 3,3,3,3,3,3,3,3,3}, 910);
        ChunkEntry chunkB2 = new ChunkEntry(new byte[] { 4,4,4,4,4,4,4,4,4}, 1112);
        
        newDatabaseVersion.addChunk(chunkA1);
        newDatabaseVersion.addChunk(chunkA2);
        newDatabaseVersion.addChunk(chunkA3);
        newDatabaseVersion.addChunk(chunkA4);
        newDatabaseVersion.addChunk(chunkB1);
        newDatabaseVersion.addChunk(chunkB2);        
        
        // Distribute chunks to file contents    	
        FileContent contentA = new FileContent();        
        contentA.addChunk(new ChunkEntryId(chunkA1.getChecksum()));
        contentA.addChunk(new ChunkEntryId(chunkA2.getChecksum()));
        contentA.addChunk(new ChunkEntryId(chunkA3.getChecksum()));
        contentA.addChunk(new ChunkEntryId(chunkA4.getChecksum()));
        contentA.setChecksum(new byte[]{5,5,5,4,4,5,5,5,5});              
        newDatabaseVersion.addFileContent(contentA);
                
        FileContent contentB = new FileContent();
        contentB.addChunk(new ChunkEntryId(chunkB1.getChecksum()));
        contentB.addChunk(new ChunkEntryId(chunkB2.getChecksum())); 
        contentB.setChecksum(new byte[]{1,1,1,3,3,5,5,5,5});                      
        newDatabaseVersion.addFileContent(contentB);
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		
		// Check chunks
		assertEquals("Chunk not found in database loaded.", chunkA1, loadedDatabase.getChunk(chunkA1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA2, loadedDatabase.getChunk(chunkA2.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA3, loadedDatabase.getChunk(chunkA3.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA4, loadedDatabase.getChunk(chunkA4.getChecksum()));

		assertEquals("Chunk not found in database loaded.", chunkB1, loadedDatabase.getChunk(chunkB1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkB2, loadedDatabase.getChunk(chunkB2.getChecksum()));
		
		// Check file contents
		FileContent loadedContentA = loadedDatabase.getContent(contentA.getChecksum());
		FileContent loadedContentB = loadedDatabase.getContent(contentB.getChecksum());
		
		assertEquals("File content not found in database loaded.", contentA, loadedContentA);
		assertEquals("File content not found in database loaded.", contentB, loadedContentB	);
	
		assertArrayEquals("Chunks in file content expected to be different.", contentA.getChunks().toArray(), loadedContentA.getChunks().toArray());
		assertArrayEquals("Chunks in file content expected to be different.", contentB.getChunks().toArray(), loadedContentB.getChunks().toArray());
	}
	
	@Test
	public void testWriteAndReadFileHistoryAndFileVersion() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
	
		// Create directories (no content!)

		// File A
		PartialFileHistory fileHistoryA = new PartialFileHistory();
		newDatabaseVersion.addFileHistory(fileHistoryA);
		
        FileVersion versionA1 = new FileVersion();
        versionA1.setVersion(1L);
        versionA1.setPath("Pictures/2013");
        versionA1.setName("New York Folder");
        newDatabaseVersion.addFileVersionToHistory(fileHistoryA.getFileId(), versionA1);
        
        FileVersion versionA2 = new FileVersion();
        versionA2.setVersion(2L);
        versionA2.setPath("Pictures/2013");
        versionA2.setName("New York");        
        newDatabaseVersion.addFileVersionToHistory(fileHistoryA.getFileId(), versionA2);	
		       
        // File B
		PartialFileHistory fileHistoryB = new PartialFileHistory();
		newDatabaseVersion.addFileHistory(fileHistoryB);
		
        FileVersion versionB1 = new FileVersion();
        versionB1.setVersion(1L);
        versionB1.setPath("Pictures/2013");
        versionB1.setName("Egypt Folder");
        newDatabaseVersion.addFileVersionToHistory(fileHistoryB.getFileId(), versionB1);
        
        FileVersion versionB2 = new FileVersion();
        versionB2.setVersion(2L);
        versionB2.setPath("Pictures/2013");
        versionB2.setName("Egypt");        
        newDatabaseVersion.addFileVersionToHistory(fileHistoryB.getFileId(), versionB2);	        	
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		 
		// File histories
		PartialFileHistory loadedFileHistoryA = loadedDatabase.getFileHistory(fileHistoryA.getFileId());
		PartialFileHistory loadedFileHistoryB = loadedDatabase.getFileHistory(fileHistoryB.getFileId());
		
		assertEquals("File history not found in database loaded.", fileHistoryA, loadedFileHistoryA);
		assertEquals("File history not found in database loaded.", fileHistoryB, loadedFileHistoryB);
		
		assertArrayEquals("File versions differ in loaded database.", fileHistoryA.getFileVersions().values().toArray(), 
				loadedFileHistoryA.getFileVersions().values().toArray());
		
		assertArrayEquals("File versions differ in loaded database.", fileHistoryB.getFileVersions().values().toArray(), 
				loadedFileHistoryB.getFileVersions().values().toArray());
	}		
	
	@Test
	@Ignore
	public void testWriteAndReadVectorClock() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();

		// Create new vector clock
		VectorClock vc = new VectorClock();
		
		vc.setClock("User 1", 14234234L);
		vc.setClock("User 2", 9433431232432L);
		vc.setClock("User 3", 1926402374L);
		
		newDatabaseVersion.setVectorClock(vc);
		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		
		// Check VC
		//loadedDatabase.getV
	}
		
	@Test
	@Ignore
	public void testWriteAndReadMultipleDatabaseVersions() {
		Database newDatabase = new Database();
		DatabaseVersion firstDatabaseVersion = new DatabaseVersion();
		DatabaseVersion secondDatabaseVersion = new DatabaseVersion();

		// TODO testWriteAndReadMultipleDatabaseVersions
	}
	
	private Database writeReadAndCompareDatabase(Database writtenDatabase) throws IOException {
		File writtenDatabaseFile = writeDatabaseFileToDisk(writtenDatabase);
		Database readDatabase = readDatabaseFileFromDisk(writtenDatabaseFile);
		
		compareDatabases(writtenDatabase, readDatabase);
		
		return readDatabase;
	}		

	private File writeDatabaseFileToDisk(Database db) throws IOException {
		File writtenDatabaseFile = new File(tempDir+"/db-"+Math.random()+"-" + generator.nextInt(Integer.MAX_VALUE));
		
		DatabaseDAO dao = new DatabaseXmlDAO();
		dao.save(db, writtenDatabaseFile);
		
		return writtenDatabaseFile;
	}
	
	private Database readDatabaseFileFromDisk(File databaseFile) throws IOException {
		Database db = new Database();
		
		DatabaseDAO dao = new DatabaseXmlDAO();
		RemoteDatabaseFile rdbf = new RemoteDatabaseFile(databaseFile);
		dao.load(db, rdbf);
		
		return db;
	}
	
	private void compareDatabases(Database writtenDatabase, Database readDatabase) {
		List<DatabaseVersion> writtenDatabaseVersions = writtenDatabase.getDatabaseVersions();
		List<DatabaseVersion> readDatabaseVersions = readDatabase.getDatabaseVersions();
		
		assertEquals("Different number of database versions.", writtenDatabaseVersions.size(), readDatabaseVersions.size());
			
		for (DatabaseVersion writtenDatabaseVersion : writtenDatabaseVersions) {
			DatabaseVersion readDatabaseVersion = null;
			
			for (DatabaseVersion aReadDatabaseVersion : readDatabaseVersions) {
				if (aReadDatabaseVersion.equals(writtenDatabaseVersion)) {
					readDatabaseVersion = aReadDatabaseVersion;
					break;
				}
			}
			
			assertNotNull("Database version "+writtenDatabaseVersion+" does not exist in read database.", readDatabaseVersion);
			
			compareDatabaseVersions(writtenDatabaseVersion, readDatabaseVersion);
		}
	}	
	
	private void compareDatabaseVersions(DatabaseVersion writtenDatabaseVersion, DatabaseVersion readDatabaseVersion) {
		compareDatabaseVersionVectorClocks(writtenDatabaseVersion.getVectorClock(), readDatabaseVersion.getVectorClock());
		compareDatabaseVersionChunks(writtenDatabaseVersion.getChunks(), readDatabaseVersion.getChunks());
		compareDatabaseVersionMultiChunks(writtenDatabaseVersion.getMultiChunks(), readDatabaseVersion.getMultiChunks());
		compareDatabaseVersionFileContents(writtenDatabaseVersion.getFileContents(), readDatabaseVersion.getFileContents());
		compareDatabaseVersionFileHistories(writtenDatabaseVersion.getFileHistories(), readDatabaseVersion.getFileHistories());	
	}		

	private void compareDatabaseVersionVectorClocks(VectorClock writtenVectorClock, VectorClock readVectorClock) {
		assertEquals("Vector clocks differ.", writtenVectorClock, readVectorClock);		
	}

	private void compareDatabaseVersionChunks(Collection<ChunkEntry> writtenChunks, Collection<ChunkEntry> readChunks) {	
		assertEquals("Different amount of Chunk objects.", writtenChunks.size(), readChunks.size());
		assertTrue("Chunk objects in written/read database version different.", writtenChunks.containsAll(readChunks));
		//assertCollectionEquals("Chunk objects in written/read database version different.", writtenChunks, readChunks);
	}
	
	private void compareDatabaseVersionMultiChunks(Collection<MultiChunkEntry> writtenMultiChunks, Collection<MultiChunkEntry> readMultiChunks) {
		assertEquals("Different amount of MultiChunk objects.", writtenMultiChunks.size(), readMultiChunks.size());
		assertTrue("MultiChunk objects in written/read database version different.", writtenMultiChunks.containsAll(readMultiChunks));
		//assertCollectionEquals("MultiChunk objects in written/read database version different.", writtenMultiChunks, readMultiChunks);
	}	
	
	private void compareDatabaseVersionFileContents(Collection<FileContent> writtenFileContents, Collection<FileContent> readFileContents) {
		assertEquals("Different amount of FileContent objects.", writtenFileContents.size(), readFileContents.size());
		assertTrue("FileContent objects in written/read database version different.", writtenFileContents.containsAll(readFileContents));
		//assertCollectionEquals("FileContent objects in written/read database version different.", writtenFileContents, readFileContents);
	}	

	private void compareDatabaseVersionFileHistories(Collection<PartialFileHistory> writtenFileHistories, Collection<PartialFileHistory> readFileHistories) {
		TestAssertUtil.assertCollectionEquals("FileHistory objects in written/read database version different.", writtenFileHistories, readFileHistories);
	}	
}
