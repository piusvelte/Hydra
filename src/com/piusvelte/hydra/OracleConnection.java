/*
 * Hydra
 * Copyright (C) 2012 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.hydra;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import oracle.jdbc.pool.OracleDataSource;

public class OracleConnection extends DatabaseConnection {

	protected OracleDataSource mDataSource;
	protected Connection mConnection;
	private static final String SIMPLE_QUERY_FORMAT = "SELECT %s FROM %s";
	private static final String SELECTION_QUERY_FORMAT = "SELECT %s FROM %s WHERE %s";
	private static final String INSERT_QUERY = "INSERT INTO %s (%s) VALUES (%s)";
	private static final String UPDATE_QUERY = "UPDATE %s SET %s WHERE %s";
	private static final String DELETE_QUERY = "DELETE FROM %s WHERE %s";

	public OracleConnection(String hostName, int hostPort, String accountPath, String username, String password) {
		super(hostName, hostPort, accountPath, username, password);
	}

	@Override
	public boolean connect() throws Exception {
		super.connect();
		if (mConnection == null) {
			StringBuilder connectionString = new StringBuilder();
			connectionString.append("jdbc:oracle:oci8:@");
			connectionString.append(mHostName);
			connectionString.append(":");
			connectionString.append(mHostPort);
			connectionString.append("/");
			connectionString.append(mAccountPath);
			mDataSource = new OracleDataSource();
			mDataSource.setUser(mUsername);
			mDataSource.setPassword(mPassword);
			mDataSource.setDriverType("oci8");
			mDataSource.setNetworkProtocol("ipc");
			mDataSource.setURL(connectionString.toString());
			mConnection = mDataSource.getConnection();
		}
		return mLock;
	}

	@Override
	public void disconnect() throws Exception {
		super.disconnect();
		if (mDataSource != null) {
			if (mConnection != null)
				mConnection.close();
			mDataSource.close();
		}
	}
	
	private JSONArray getResult(ResultSet rs) throws SQLException {
		JSONArray rows = new JSONArray();
		ResultSetMetaData rsmd = rs.getMetaData();
		String[] columnsArr = new String[rsmd.getColumnCount()];
		for (int c = 0, l = columnsArr.length; c < l; c++)
			columnsArr[c] = rsmd.getColumnName(c);
		while (rs.next()) {
			JSONArray rowData = new JSONArray();
			for (String column : columnsArr)
				rowData.add((String) rs.getObject(column));
			rows.add(rowData);
		}
		return rows;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject execute(String statement) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		Statement s = null;
		ResultSet rs = null;
		try {
			s = mConnection.createStatement();
			rs = s.executeQuery(statement);
			response.put("result", getResult(rs));
		} catch (SQLException e) {
			errors.add(e.getMessage());
		} finally {
			if (s != null) {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						errors.add(e.getMessage());
					}
				}
				try {
					s.close();
				} catch (SQLException e) {
					errors.add(e.getMessage());
				}
			}
		}
		response.put("errors", errors);
		if (!response.containsKey("result")) {
			JSONArray rows = new JSONArray();
			JSONArray rowData = new JSONArray();
			rows.add(rowData);
			response.put("result", rows);
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject query(String object, String[] columns, String selection) {
		Statement s = null;
		ResultSet rs = null;
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0, l = columns.length; i < l; i++) {
				if (i > 0)
					sb.append(",");
				sb.append(columns[i].replaceAll("\\.", "_"));
			}
			String columnsStr = sb.toString();
			s = mConnection.createStatement();
			if (selection != null)
				rs = s.executeQuery(String.format(SELECTION_QUERY_FORMAT, columnsStr, object, selection).toString());
			else
				rs = s.executeQuery(String.format(SIMPLE_QUERY_FORMAT, columnsStr, object).toString());
			response.put("result", getResult(rs));
		} catch (SQLException e) {
			errors.add(e.getMessage());
		} finally {
			if (s != null) {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						errors.add(e.getMessage());
					}
				}
				try {
					s.close();
				} catch (SQLException e) {
					errors.add(e.getMessage());
				}
			}
		}
		response.put("errors", errors);
		if (!response.containsKey("result")) {
			JSONArray rows = new JSONArray();
			JSONArray rowData = new JSONArray();
			rows.add(rowData);
			response.put("result", rows);
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject insert(String object, String[] columns, String[] values) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		Statement s = null;
		ResultSet rs = null;
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0, l = columns.length; i < l; i++) {
				if (i > 0)
					sb.append(",");
				sb.append(columns[i].replaceAll("\\.", "_"));
			}
			String columnsStr = sb.toString();
			sb = new StringBuilder();
			for (int i = 0, l = values.length; i < l; i++) {
				if (i > 0)
					sb.append(",");
				sb.append(values[i]);
			}
			String valuesStr = sb.toString();
			s = mConnection.createStatement();
			rs = s.executeQuery(String.format(INSERT_QUERY, object, columnsStr, valuesStr).toString());
			response.put("result", getResult(rs));
		} catch (SQLException e) {
			errors.add(e.getMessage());
		} finally {
			if (s != null) {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						errors.add(e.getMessage());
					}
				}
				try {
					s.close();
				} catch (SQLException e) {
					errors.add(e.getMessage());
				}
			}
		}
		response.put("errors", errors);
		if (!response.containsKey("result")) {
			JSONArray rows = new JSONArray();
			JSONArray rowData = new JSONArray();
			rows.add(rowData);
			response.put("result", rows);
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject update(String object, String[] columns, String[] values, String selection) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		Statement s = null;
		ResultSet rs = null;
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 0, l = columns.length; i < l; i++) {
				if (i > 0)
					sb.append(",");
				sb.append(columns[i].replaceAll("\\.", "_"));
				sb.append("=");
				sb.append(values[i]);
			}
			s = mConnection.createStatement();
			rs = s.executeQuery(String.format(UPDATE_QUERY, object, sb.toString(), selection).toString());
			response.put("result", getResult(rs));
		} catch (SQLException e) {
			errors.add(e.getMessage());
		} finally {
			if (s != null) {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						errors.add(e.getMessage());
					}
				}
				try {
					s.close();
				} catch (SQLException e) {
					errors.add(e.getMessage());
				}
			}
		}
		response.put("errors", errors);
		if (!response.containsKey("result")) {
			JSONArray rows = new JSONArray();
			JSONArray rowData = new JSONArray();
			rows.add(rowData);
			response.put("result", rows);
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject delete(String object, String selection) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		Statement s = null;
		ResultSet rs = null;
		try {
			s = mConnection.createStatement();
			rs = s.executeQuery(String.format(DELETE_QUERY, object, selection).toString());
			response.put("result", getResult(rs));
		} catch (SQLException e) {
			errors.add(e.getMessage());
		} finally {
			if (s != null) {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						errors.add(e.getMessage());
					}
				}
				try {
					s.close();
				} catch (SQLException e) {
					errors.add(e.getMessage());
				}
			}
		}
		response.put("errors", errors);
		if (!response.containsKey("result")) {
			JSONArray rows = new JSONArray();
			JSONArray rowData = new JSONArray();
			rows.add(rowData);
			response.put("result", rows);
		}
		return response;
	}

}
