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

package org.roqmessaging.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaAPI {
	private Client oneConnection = null;
	private Logger logger = Logger.getLogger(OpenNebulaAPI.class);
	//maintain a list of vm allocated
	private List<VirtualMachine> vmAllocated =null;

	/**
	 * Create a link to the OpenNebula Infrastructure
	 * @param user User to connect on OpenNebula RPC
	 * @param password Password to connect on OpenNebula RPC 
	 * @param endpoint URL of the RPC endpoint
	 */
	public OpenNebulaAPI(String user, String password, String endpoint) {
		try {
			this.vmAllocated=new ArrayList<VirtualMachine>();
			this.oneConnection = new Client(user + ":" + password,
					endpoint);
		} catch (ClientConfigurationException e) {
			logger.error("Error when trying to connect to inferno.local", e);
		}
	}

	
	/**
	 * This function allows you to create VM with the template specified
	 * @param gcmadress Address of the Global Configuration Manager
	 * @return vm VM is the just created VM
	 */
	public int createInstance(String gcmadress) {

		String vmTemplate = "NAME=vm-4-RoQ\n" + "CONTEXT=[\n"
				+ "FILES=\"/nebuladata/scripts/init.sh\",\n"
				+ "GATEWAY=\"192.168.0.1\",\n" + "HOSTNAME=\"RoQ-VM-$VMID\",\n"
				//+ "IP_PUBLIC=$NIC[IP, NETWORK=\"RoQ\"],\n"
				+ "TARGET=\"vdb\",\n" + "GCMIP=\"" + gcmadress + "\"]\n"
				+ "CPU=0.2\n" + "DISK=[\n" + "IMAGE=\"Base Instance\",\n"
				+ "TARGET=\"vda\" ]\n" + "FEATURES=[\n" + "ACPI=\"yes\" ]\n"
				+ "GRAPHICS=[\n" + "KEYMAP=\"fr\",\n" + "LISTEN=\"0.0.0.0\",\n"
				+ "TYPE=\"vnc\" ]\n" + "MEMORY=512\n" + "NIC=[\n"
				+ "NETWORK=\"RoQ\" ]\n" + "OS=[\n"
				+ "ARCH=\"x86_64\",\n" + "BOOT=\"hd\" ]";

		logger.info("Trying to allocate the virtual machine... ");
		OneResponse rc = VirtualMachine
				.allocate(this.oneConnection, vmTemplate);
		
		if (rc.isError()) {
			logger.error("Failed to allocate VM !" + rc.getErrorMessage() + "\n" + vmTemplate);
		} else {
			// The response message is the new VM's ID
			int newVMID = Integer.parseInt(rc.getMessage());
			logger.info("ok, ID " + newVMID + ".");
			VirtualMachine vm = new VirtualMachine(newVMID, oneConnection);
			vmAllocated.add(vm);
			return newVMID;
		}
		
		return -1;
		
	}
	/**
	 * This function allows you to delete one VM at a time
	 * @param vmID ID of the VM to delete
	 * @throws IllegalStateException sent when unable to delete the VM
	 */
	public void deleteInstance(int vmID) throws IllegalStateException{
		VirtualMachine vm = new VirtualMachine(vmID, oneConnection);
		logger.info("\nTrying to delete the VM : " + vm.getId());
		try {
			this.deleteInstanceByVM(vm);
		} catch (IllegalStateException e) {
			throw e;
		}
		
	}
	
	
	/**
	 * This function is generic and used to delete the VM in parameter 
	 * @param vm is the VM representation created by the allocation of a VM
	 * @throws IllegalStateException 
	 */
	private void deleteInstanceByVM(VirtualMachine vm) throws IllegalStateException {
		OneResponse rc = vm.finalizeVM();
		if (rc.isError()) {
			logger.error("Failed to delete " + vm.getId() + " : "
					+ rc.getErrorMessage() + "\n");
			throw new IllegalStateException("Failed to delete " + vm.getId());
		} else {
			// No response message from the API if successful !
			logger.info("Deleted : " + vm.getId() + ".");
			this.vmAllocated.remove(vm);
		}
	}
	
	
	/**
	 * This function delete all the VMs previously allocated
	 * @throws IllegalStateException
	 */
	public void cleanAllInstances() throws IllegalStateException{
		for (VirtualMachine vm : this.getVmToDelete()) {
			try {
				this.deleteInstanceByVM(vm);
			} catch (IllegalStateException e) {
				throw e;
			}
		}
	}
	
	/**
	 * @return a working copy of the array containing the same vm objects.
	 */
	private List<VirtualMachine> getVmToDelete(){
		List<VirtualMachine> vmCopy = new ArrayList<VirtualMachine>();
		for (VirtualMachine vm : this.vmAllocated) {
			vmCopy.add(vm);
		}
		return vmCopy;
	}
}
