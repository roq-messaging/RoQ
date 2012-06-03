/**
 * Copyright 2012 EURANOVA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.roqmessaging.management.config.internal;

/**
 * Class GCMPropertyDAO
 * <p> Description: data object representing the file configuration of the GCM.
 * 
 * @author sskhiri
 */
public class GCMPropertyDAO {
	
	//The frequency period for the update to the configuration server
	private int period = 60000;
	
	//Define whether we format the DB
	private boolean formatDB = false;

	/**
	 * @return the period
	 */
	public int getPeriod() {
		return period;
	}

	/**
	 * @param period the period to set
	 */
	public void setPeriod(int period) {
		this.period = period;
	}

	/**
	 * @return the formatDB
	 */
	public boolean isFormatDB() {
		return formatDB;
	}

	/**
	 * @param formatDB the formatDB to set
	 */
	public void setFormatDB(boolean formatDB) {
		this.formatDB = formatDB;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GCM "+ this.getPeriod()+" formatDB "+ this.isFormatDB();
	}
}
