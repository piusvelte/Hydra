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

import static com.piusvelte.hydra.ApiServlet.ACTION_DELETE;
import static com.piusvelte.hydra.ApiServlet.ACTION_EXECUTE;
import static com.piusvelte.hydra.ApiServlet.ACTION_INSERT;
import static com.piusvelte.hydra.ApiServlet.ACTION_QUERY;
import static com.piusvelte.hydra.ApiServlet.ACTION_SUBROUTINE;
import static com.piusvelte.hydra.ApiServlet.ACTION_UPDATE;

import org.json.simple.parser.ParseException;

public class QueueThread extends Thread {

	public static final int DEFAULT_QUEUERETRYINTERVAL = 300000;
	protected int mQueueRetryInterval = DEFAULT_QUEUERETRYINTERVAL;
	private boolean mKeepAlive = true;
	private ConnectionManager hydraService;

	public QueueThread(ConnectionManager hydraService, int queueRetryInterval) {
		this.hydraService = hydraService;
		mQueueRetryInterval = queueRetryInterval;
	}

	@Override
	public void run() {
		while (mKeepAlive) {
			String request = hydraService.dequeueRequest();
			// keep track of the beginning of the queue
			String firstRequeuedRequest = null;
			while (mKeepAlive && (request != null) && (!request.equals(firstRequeuedRequest))) {
				// process the request
				HydraRequest hydraRequest = null;
				try {
					hydraRequest = new HydraRequest(request);
				} catch (ParseException e) {
					hydraRequest = null;
					e.printStackTrace();
				}
				if ((hydraRequest != null) && (hydraRequest.database != null) && (!hydraService.getDatabase(hydraRequest.database).isEmpty())) {
					DatabaseConnection databaseConnection = null;
					try {
						databaseConnection = hydraService.getDatabaseConnection(hydraRequest.database);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (databaseConnection == null) {
						// still not available, re-queue
						if (hydraRequest.queueable) {
							if (firstRequeuedRequest == null)
								firstRequeuedRequest = hydraRequest.toJSONString();
							hydraService.requeueRequest(hydraRequest.toJSONString());
						}
					} else {
						if (ACTION_EXECUTE.equals(hydraRequest.action) && (hydraRequest.target.length() > 0))
							databaseConnection.execute(hydraRequest.target);
						else if (ACTION_QUERY.equals(hydraRequest.action) && (hydraRequest.target.length() > 0) && (hydraRequest.columns.length > 0))
							databaseConnection.query(hydraRequest.target, hydraRequest.columns, hydraRequest.selection);
						else if (ACTION_UPDATE.equals(hydraRequest.action) && (hydraRequest.target.length() > 0) && (hydraRequest.columns.length > 0) && (hydraRequest.values.length > 0))
							databaseConnection.update(hydraRequest.target, hydraRequest.columns, hydraRequest.values, hydraRequest.selection);
						else if (ACTION_INSERT.equals(hydraRequest.action) && (hydraRequest.target.length() > 0) && (hydraRequest.columns.length > 0) && (hydraRequest.values.length > 0))
							databaseConnection.insert(hydraRequest.target, hydraRequest.columns, hydraRequest.values);
						else if (ACTION_DELETE.equals(hydraRequest.action) && (hydraRequest.target.length() > 0))
							databaseConnection.delete(hydraRequest.target, hydraRequest.selection);
						else if (ACTION_SUBROUTINE.equals(hydraRequest.action) && (hydraRequest.target.length() > 0) && (hydraRequest.values.length > 0))
							databaseConnection.subroutine(hydraRequest.target, hydraRequest.values);
						// release the connection
						databaseConnection.release();
						// successfully processed, remove from the queue
						hydraService.requeueRequest(null);
					}
				} else
					hydraService.requeueRequest(null); // bad request
				request = hydraService.dequeueRequest();
			}
			// was anything requeued, else shutdown
			if (firstRequeuedRequest == null)
				mKeepAlive = false;
			else {
				try {
					Thread.sleep(mQueueRetryInterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void shutdown() {
		mKeepAlive = false;
	}
}
