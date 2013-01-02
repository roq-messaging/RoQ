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

	public OpenNebulaAPI() {
		try {
			this.vmAllocated=new ArrayList<VirtualMachine>();
			this.oneConnection = new Client("roq:roq",
					"http://inferno.local:2633/RPC2");
		} catch (ClientConfigurationException e) {
			logger.error("Error when trying to connect to inferno.local", e);
		}
	}

	/**
	 * @return 
 * 
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
	 * 
	 */
	public void deleteInstance(int vmID) throws IllegalStateException{
		VirtualMachine vm = new VirtualMachine(vmID, oneConnection);
		logger.info("\nTrying to delete the VM : " + vm.getId());
		OneResponse rc = vm.finalizeVM();
		
		if (rc.isError()) {
			logger.error("Failed to delete " + vm.getId() + " : " + rc.getErrorMessage() + "\n");
			throw new IllegalStateException("Failed to delete " + vm.getId());
		} else {
			// No response message from the API if successful !
			logger.info("Deleted : " + vm.getId() + ".");
			this.vmAllocated.remove(vm);
		}
	}
	
	
}
