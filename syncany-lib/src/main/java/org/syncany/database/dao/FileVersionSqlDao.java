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
package org.syncany.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;

/**
 * The file version DAO queries and modifies the <i>fileversion</i> in
 * the SQL database. This table corresponds to the Java object {@link FileVersion}.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileVersionSqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(FileVersionSqlDao.class.getSimpleName());
	
	public FileVersionSqlDao(Connection connection) {
		super(connection);
	}
	
	/**
	 * Writes a list of {@link FileVersion} to the database table <i>fileversion</i> using <tt>INSERT</tt>s
	 * and the given connection.
	 * 
	 * <p><b>Note:</b> This method executes, but <b>does not commit</b> the queries.
	 * 
	 * @param connection The connection used to execute the statements
	 * @param fileHistoryId References the {@link PartialFileHistory} to which the list of file versions belongs 
	 * @param databaseVersionId References the {@link PartialFileHistory} to which the list of file versions belongs
	 * @param fileVersions List of {@link FileVersion}s to be written to the database
	 * @throws SQLException If the SQL statement fails
	 */
	public void writeFileVersions(Connection connection, FileHistoryId fileHistoryId, long databaseVersionId, Collection<FileVersion> fileVersions) throws SQLException {
		PreparedStatement preparedStatement = getStatement(connection, "/sql/fileversion.insert.writeFileVersions.sql");

		for (FileVersion fileVersion : fileVersions) {
			String fileContentChecksumStr = (fileVersion.getChecksum() != null) ? fileVersion.getChecksum().toString() : null;					  		

			preparedStatement.setString(1, fileHistoryId.toString());
			preparedStatement.setInt(2, Integer.parseInt(""+fileVersion.getVersion()));
			preparedStatement.setLong(3, databaseVersionId);
			preparedStatement.setString(4, fileVersion.getPath());
			preparedStatement.setString(5, fileVersion.getType().toString());
			preparedStatement.setString(6, fileVersion.getStatus().toString());
			preparedStatement.setLong(7, fileVersion.getSize());
			preparedStatement.setTimestamp(8, new Timestamp(fileVersion.getLastModified().getTime()));
			preparedStatement.setString(9, fileVersion.getLinkTarget());
			preparedStatement.setString(10, fileContentChecksumStr);
			preparedStatement.setTimestamp(11, new Timestamp(fileVersion.getUpdated().getTime()));
			preparedStatement.setString(12, fileVersion.getPosixPermissions());
			preparedStatement.setString(13, fileVersion.getDosAttributes());
			
			preparedStatement.addBatch();
		}				
		
		preparedStatement.executeBatch();
		preparedStatement.close();
	}

	/**
	 * Removes {@link FileVersion}s from the database table <i>fileversion</i> for which the 
	 * the corresponding database is marked <tt>DIRTY</tt>. 
	 * 
	 * <p><b>Note:</b> This method executes, but does not commit the query.
	 * 
	 * @throws SQLException If the SQL statement fails
	 */	
	public void removeDirtyFileVersions() throws SQLException {
		try (PreparedStatement preparedStatement = getStatement("/sql/fileversion.delete.dirty.removeDirtyFileVersions.sql")) {
			preparedStatement.executeUpdate();
		}
	}
	
	/**
	 * Removes all file versions with versions <b>lower or equal</b> than the given file version.
	 * 
	 * <p>Note that this method does not just delete the given file version, but also all of its
	 * previous versions.
	 */
	public void removeFileVersions(Map<FileHistoryId, FileVersion> purgeFileVersions) throws SQLException {
		if (purgeFileVersions.size() > 0) {
			try (PreparedStatement preparedStatement = getStatement(connection, "/sql/fileversion.delete.all.removeFileVersionsByIds.sql")) {
				for (Map.Entry<FileHistoryId, FileVersion> purgeFileVersionEntry : purgeFileVersions.entrySet()) {
					FileHistoryId purgeFileHistoryId = purgeFileVersionEntry.getKey();
					FileVersion purgeFileVersion = purgeFileVersionEntry.getValue();
					
					preparedStatement.setString(1, purgeFileHistoryId.toString());
					preparedStatement.setLong(2, purgeFileVersion.getVersion());
					
					preparedStatement.addBatch();
				}				
				
				preparedStatement.executeBatch();
			}
		}
	}

	public void removeDeletedVersions() throws SQLException {
		try (PreparedStatement preparedStatement = getStatement("/sql/fileversion.delete.all.removeDeletedVersions.sql")) {	
			preparedStatement.executeUpdate();
		}
	}
	
	/**
	 * Queries the database for the currently active {@link FileVersion}s and returns it
	 * as a map. If the current file tree (on the disk) has not changed, the result will
	 * match the files on the disk.
	 * 
	 * <p>Keys in the returned map correspond to the file version's relative file path,
	 * and values to the actual {@link FileVersion} object.
	 * 
	 * @return Returns the current file tree as a map of relative paths to {@link FileVersion} objects
	 */
	public Map<String, FileVersion> getCurrentFileTree() {		
		try (PreparedStatement preparedStatement = getStatement("/sql/fileversion.select.master.getCurrentFileTree.sql")) {
			return getFileTree(preparedStatement);				
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Queries the database for the {@link FileVersion}s active at the given date and
	 * returns it as a map.
	 * 
	 * <p>Keys in the returned map correspond to the file version's relative file path,
	 * and values to the actual {@link FileVersion} object.
	 * 
	 * @param date Date for which the file tree should be queried
	 * @return Returns the file tree at the given date as a map of relative paths to {@link FileVersion} objects
	 */
	public Map<String, FileVersion> getFileTreeAtDate(Date date) {		
		try (PreparedStatement preparedStatement = getStatement("/sql/fileversion.select.master.getFileTreeAtDate.sql")) {
			preparedStatement.setTimestamp(1, new Timestamp(date.getTime()));
			preparedStatement.setTimestamp(2, new Timestamp(date.getTime()));
			
			return getFileTree(preparedStatement);					
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	

	/**
	 * Queries the database for the {@link FileVersion}s active at the given fileVersionNumber and
	 * returns it as a map.
	 * 
	 * <p>Keys in the returned map correspond to the file version's relative file path,
	 * and values to the actual {@link FileVersion} object.
	 * 
	 * @param fileVersionNumber Integer for which the file tree should be queried
	 * @return Returns the file tree at the given date as a map of relative paths to {@link FileVersion} objects
	 */
	public Map<String, FileVersion> getFileTreeAtVersion(Integer fileVersionNumber) {		
		try (PreparedStatement preparedStatement = getStatement("/sql/fileversion.select.master.getFileTreeAtVersion.sql")) {
			preparedStatement.setInt(1, fileVersionNumber.intValue());
			preparedStatement.setInt(2, fileVersionNumber.intValue());
			
			return getFileTree(preparedStatement);					
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<FileHistoryId, FileVersion> getFileHistoriesWithMostRecentPurgeVersion(int keepVersionsCount) {
		try (PreparedStatement preparedStatement = getStatement("/sql/fileversion.select.all.getMostRecentPurgeVersions.sql")) {
			preparedStatement.setInt(1, keepVersionsCount);
			preparedStatement.setInt(2, keepVersionsCount);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				Map<FileHistoryId, FileVersion> mostRecentPurgeFileVersions = new HashMap<FileHistoryId, FileVersion>();
				
				while (resultSet.next()) {
					FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
					FileVersion fileVersion = createFileVersionFromRow(resultSet);
					
					mostRecentPurgeFileVersions.put(fileHistoryId, fileVersion);
				}	 
				
				return mostRecentPurgeFileVersions;
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Map<String, FileVersion> getFileTree(PreparedStatement preparedStatement) {
		Map<String, FileVersion> fileTree = new HashMap<String, FileVersion>();

		try (ResultSet resultSet = preparedStatement.executeQuery()) {
			while (resultSet.next()) {
				FileVersion fileVersion = createFileVersionFromRow(resultSet);
				fileTree.put(fileVersion.getPath(), fileVersion);
			}

			return fileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}		
	
	@Deprecated
	public FileVersion getFileVersionByPath(String path) {
		try (PreparedStatement preparedStatement = getStatement("/sql/fileversion.select.master.getFileVersionByPath.sql")) {
			preparedStatement.setString(1, path);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return createFileVersionFromRow(resultSet);
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Deprecated
	public FileVersion getFileVersionByFileHistoryId(FileHistoryId fileHistoryId) {
		try (PreparedStatement preparedStatement = getStatement("/sql/fileversion.select.master.getFileVersionByFileHistoryId.sql")) {
			preparedStatement.setString(1, fileHistoryId.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return createFileVersionFromRow(resultSet);
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	// TODO [low] This should be private; but it has to be public for a test
	public FileVersion createFileVersionFromRow(ResultSet resultSet) throws SQLException {
		FileVersion fileVersion = new FileVersion();

		fileVersion.setVersion(resultSet.getLong("version"));
		fileVersion.setPath(resultSet.getString("path"));
		fileVersion.setType(FileType.valueOf(resultSet.getString("type")));
		fileVersion.setStatus(FileStatus.valueOf(resultSet.getString("status")));
		fileVersion.setSize(resultSet.getLong("size"));
		fileVersion.setLastModified(new Date(resultSet.getTimestamp("lastmodified").getTime()));

		if (resultSet.getString("linktarget") != null) {
			fileVersion.setLinkTarget(resultSet.getString("linktarget"));
		}

		if (resultSet.getString("filecontent_checksum") != null) {
			FileChecksum fileChecksum = FileChecksum.parseFileChecksum(resultSet.getString("filecontent_checksum"));
			fileVersion.setChecksum(fileChecksum);
		}

		if (resultSet.getString("updated") != null) {
			fileVersion.setUpdated(new Date(resultSet.getTimestamp("updated").getTime()));
		}

		if (resultSet.getString("posixperms") != null) {
			fileVersion.setPosixPermissions(resultSet.getString("posixperms"));
		}

		if (resultSet.getString("dosattrs") != null) {
			fileVersion.setDosAttributes(resultSet.getString("dosattrs"));
		}

		return fileVersion;
	}
}
