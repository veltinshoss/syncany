/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.dao.ApplicationSqlDao;
import org.syncany.database.dao.ChunkSqlDao;
import org.syncany.database.dao.DatabaseVersionSqlDao;
import org.syncany.database.dao.FileContentSqlDao;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.database.dao.MultiChunkSqlDao;
import org.syncany.operations.down.DatabaseBranch;

/**
 * Represents the single entry point for all SQL database queries.
 * 
 * <p>This class combines all specific SQL database data access objects (DAOs) into
 * a single class, and forwards all method calls to the responsible DAO.  
 * 
 * @see {@link ApplicationSqlDao}
 * @see {@link ChunkSqlDao}
 * @see {@link FileContentSqlDao}
 * @see {@link FileVersionSqlDao}
 * @see {@link FileHistorySqlDao}
 * @see {@link MultiChunkSqlDao}
 * @see {@link DatabaseVersionSqlDao}
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class SqlDatabase {
	protected static final Logger logger = Logger.getLogger(SqlDatabase.class.getSimpleName());

	protected Connection connection;
	protected ApplicationSqlDao applicationDao;
	protected ChunkSqlDao chunkDao;
	protected FileContentSqlDao fileContentDao;
	protected FileVersionSqlDao fileVersionDao;
	protected FileHistorySqlDao fileHistoryDao;
	protected MultiChunkSqlDao multiChunkDao;
	protected DatabaseVersionSqlDao databaseVersionDao;

	public SqlDatabase(Config config) {
		this.connection = config.createDatabaseConnection();
		this.applicationDao = new ApplicationSqlDao(connection);
		this.chunkDao = new ChunkSqlDao(connection);
		this.fileContentDao = new FileContentSqlDao(connection);
		this.fileVersionDao = new FileVersionSqlDao(connection);
		this.fileHistoryDao = new FileHistorySqlDao(connection, fileVersionDao);
		this.multiChunkDao = new MultiChunkSqlDao(connection);
		this.databaseVersionDao = new DatabaseVersionSqlDao(connection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
	}

	// General
	
	public void commit() throws SQLException {
		connection.commit();
	}

	public void removeUnreferencedDatabaseEntities() {
		try {
			removeUnreferencedFileHistories();
			removeUnreferencedFileContents();
			removeUnreferencedMultiChunks();
			removeUnreferencedChunks();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	// Application

	public void writeKnownRemoteDatabases(List<DatabaseRemoteFile> remoteDatabases) throws SQLException {
		applicationDao.writeKnownRemoteDatabases(remoteDatabases);
	}

	public List<DatabaseRemoteFile> getKnownDatabases() {
		return applicationDao.getKnownDatabases();
	}

	public void shutdown() {
		applicationDao.shutdown();
	}

	// Database version

	public Iterator<DatabaseVersion> getDirtyDatabaseVersions() {
		return databaseVersionDao.getDirtyDatabaseVersions();
	}

	public Iterator<DatabaseVersion> getDatabaseVersionsTo(String machineName, long maxLocalClientVersion) {
		return databaseVersionDao.getDatabaseVersionsTo(machineName, maxLocalClientVersion);
	}

	public DatabaseVersionHeader getLastDatabaseVersionHeader() {
		return databaseVersionDao.getLastDatabaseVersionHeader();
	}

	public DatabaseBranch getLocalDatabaseBranch() {
		return databaseVersionDao.getLocalDatabaseBranch();
	}

	public long persistDatabaseVersion(DatabaseVersion databaseVersion) {
		return databaseVersionDao.persistDatabaseVersion(databaseVersion);
	}
	
	public void writeDatabaseVersionHeader(DatabaseVersionHeader databaseVersionHeader) throws SQLException {
		databaseVersionDao.writeDatabaseVersionHeader(databaseVersionHeader);
	}

	public void markDatabaseVersionDirty(VectorClock vectorClock) {
		databaseVersionDao.markDatabaseVersionDirty(vectorClock);
	}

	public void removeDirtyDatabaseVersions(long newDatabaseVersionId) {
		databaseVersionDao.removeDirtyDatabaseVersions(newDatabaseVersionId);
	}

	public Long getMaxDirtyVectorClock(String machineName) {
		return databaseVersionDao.getMaxDirtyVectorClock(machineName);
	}

	// File History

	@Deprecated	
	public List<PartialFileHistory> getFileHistoriesWithFileVersions() {
		// TODO [medium] Note: This returns the full database. Don't use this!
		return fileHistoryDao.getFileHistoriesWithFileVersions();
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersion() {
		return fileHistoryDao.getFileHistoriesWithLastVersion();
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersionByChecksum(FileChecksum fileContentChecksum) {
		return fileHistoryDao.getFileHistoriesWithLastVersionByChecksum(fileContentChecksum);
	}
	
	private void removeUnreferencedFileHistories() throws SQLException {
		fileHistoryDao.removeUnreferencedFileHistories();
	}

	// File Version

	public Map<String, FileVersion> getCurrentFileTree() {
		return fileVersionDao.getCurrentFileTree();
	}
	
	public void removeSmallerOrEqualFileVersions(Map<FileHistoryId, FileVersion> purgeFileVersions) throws SQLException {
		fileVersionDao.removeFileVersions(purgeFileVersions);
	}
	
	public void removeDeletedFileVersions() throws SQLException {
		fileVersionDao.removeDeletedVersions();
	}

	@Deprecated
	public FileVersion getFileVersionByPath(String path) {
		return fileVersionDao.getFileVersionByPath(path);
	}

	@Deprecated
	public FileVersion getFileVersionByFileHistoryId(FileHistoryId fileHistoryId) {
		return fileVersionDao.getFileVersionByFileHistoryId(fileHistoryId);
	}

	public Map<String, FileVersion> getFileTreeAtDate(Date date) {
		return fileVersionDao.getFileTreeAtDate(date);
	}

	public Map<String, FileVersion> getFileTreeAtVersion(Integer fileVersionNumber) {
		return fileVersionDao.getFileTreeAtVersion(fileVersionNumber);
	}
	
	public Map<FileHistoryId, FileVersion> getFileHistoriesWithMostRecentPurgeVersion(int keepVersionsCount) {
		return fileVersionDao.getFileHistoriesWithMostRecentPurgeVersion(keepVersionsCount);
	}	

	// Multi Chunk

	public List<MultiChunkId> getMultiChunkIds(FileChecksum fileChecksum) {
		return multiChunkDao.getMultiChunkIds(fileChecksum);
	}

	public MultiChunkId getMultiChunkId(ChunkChecksum chunkChecksum) {
		return multiChunkDao.getMultiChunkId(chunkChecksum);
	}
	
	public Map<ChunkChecksum, MultiChunkId> getMultiChunkIdsByChecksums(List<ChunkChecksum> chunkChecksums) {
		return multiChunkDao.getMultiChunkIdsByChecksums(chunkChecksums);
	}

	public List<MultiChunkId> getDirtyMultiChunkIds() {
		return multiChunkDao.getDirtyMultiChunkIds();
	}	

	public List<MultiChunkEntry> getUnusedMultiChunks() {
		return multiChunkDao.getUnusedMultiChunks();
	}
	
	private void removeUnreferencedMultiChunks() throws SQLException {
		multiChunkDao.removeUnreferencedMultiChunks();
	}

	// Chunk

	protected Map<ChunkChecksum, ChunkEntry> getChunks(VectorClock vectorClock) {
		return chunkDao.getChunks(vectorClock);
	}

	public ChunkEntry getChunk(ChunkChecksum chunkChecksum) {
		return chunkDao.getChunk(chunkChecksum);
	}	

	private void removeUnreferencedChunks() {
		chunkDao.removeUnreferencedChunks();
	}

	// File Content

	public FileContent getFileContent(FileChecksum fileChecksum, boolean includeChunkChecksums) {
		return fileContentDao.getFileContent(fileChecksum, includeChunkChecksums);
	}

	private void removeUnreferencedFileContents() throws SQLException {
		fileContentDao.removeUnreferencedFileContents();
	}
}
