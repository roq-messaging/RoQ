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
package org.roqmessaging.core.interfaces;

/**
 * Interface  IStoppable
 * <p> Description: defines any RoQ element that can be stopped through the stop method.
 * 
 * @author sskhiri
 */
public interface IStoppable {
	
	/**
	 * Stop the current element
	 */
	public void shutDown();
	
	/**
	 * @return the name of the stoppable element.
	 */
	public String getName();

}
