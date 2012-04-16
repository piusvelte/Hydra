package com.figsolutions.hydra;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import asjava.uniclientlibs.UniDynArray;
import asjava.uniclientlibs.UniString;
import asjava.uniobjects.UniCommand;
import asjava.uniobjects.UniCommandException;
import asjava.uniobjects.UniFile;
import asjava.uniobjects.UniFileException;
import asjava.uniobjects.UniSelectList;
import asjava.uniobjects.UniSelectListException;
import asjava.uniobjects.UniSession;
import asjava.uniobjects.UniSessionException;

public class UnidataConnection extends DatabaseConnection {

	UniSession mSession;
	private static final String SIMPLE_QUERY_FORMAT = "SELECT %s";
	private static final String SELECTION_QUERY_FORMAT = "SELECT %s WITH %s";

	public UnidataConnection(String hostName, String hostPort, String accountPath, String username, String password, long timeout) {
		super(hostName, hostPort, accountPath, username, password, timeout);
	}

	@Override
	public boolean connect() throws Exception {
		super.connect();
		if (mSession == null) {
			mSession = new UniSession();
			mSession.setHostName(mHostName);
			mSession.setHostPort(Integer.parseInt(mHostPort));
			mSession.setAccountPath(mAccountPath);
			mSession.setUserName(mUsername);
			mSession.setPassword(mPassword);
			mSession.setConnectionString("udcs");
			mSession.connect();
		}
		return mLock;
	}

	@Override
	public void disconnect() throws Exception {
		super.disconnect();
		// check if client threads are waiting for a connection
		if ((mSession != null) && !HydraService.pendingConnections()) {
			mSession.disconnect();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public String execute(String statement) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		try {
			UniCommand uCommand = mSession.command();
			uCommand.setCommand(statement);
			uCommand.exec();
			response.put("result", uCommand.response());
		} catch (UniSessionException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (UniCommandException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		}
		response.put("errors", errors);
		return response.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String query(String object, String[] columns, String selection) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		UniFile uFile = null;

		try {
			JSONArray result = new JSONArray();
			UniCommand uCommand = mSession.command();
			if (selection == null) {
				uCommand.setCommand(String.format(SIMPLE_QUERY_FORMAT, object).toString());
			} else {
				uCommand.setCommand(String.format(SELECTION_QUERY_FORMAT, object, selection).toString());
			}
			UniSelectList uSelect = mSession.selectList(0);
			uCommand.exec();
			uFile = mSession.openFile(object);
			UniString recordID = null;
			while ((recordID = uSelect.next()).length() > 0) {
				uFile.setRecordID(recordID);
				for (String column : columns) {
					JSONObject col = new JSONObject();
					col.put(column, uFile.readNamedField(column).toString());
					result.add(col);
				}
			}
			response.put("result", result);
		} catch (UniSessionException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (UniCommandException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (UniFileException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (UniSelectListException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} finally {
			if (uFile != null) {
				try {
					uFile.close();
				} catch (UniFileException e) {
					errors.add(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		response.put("errors", errors);
		return response.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String insert(String object, String[] columns, String[] values) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		response.put("errors", errors);
		return response.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String update(String object, String[] columns, String[] values, String selection) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		UniFile uFile = null;

		try {
			uFile = mSession.openFile(object);
			UniCommand uCommand = mSession.command();
			uCommand.setCommand(String.format(SIMPLE_QUERY_FORMAT, object, columns).toString());
			uCommand.exec();

			//						String atFM = uSession.getMarkCharacter(UniTokens.FM);
			UniSelectList uSelect = mSession.selectList(0);

			JSONArray result = new JSONArray(); 
			while (!uSelect.isLastRecordRead()) {
				uFile.setRecordID(uSelect.next());
				for (String column : columns) {
					JSONObject col = new JSONObject();
					col.put(column, uFile.readNamedField(column));
					result.add(col);
				}
			}
			response.put("result", result);
		} catch (UniSessionException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (UniCommandException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (UniFileException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (UniSelectListException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} finally {
			if (uFile != null) {
				try {
					uFile.close();
				} catch (UniFileException e) {
					errors.add(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		response.put("errors", errors);
		return response.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String delete(String object, String selection) {
		JSONObject response = new JSONObject();
		JSONArray errors = new JSONArray();
		response.put("errors", errors);
		return response.toString();
	}

}