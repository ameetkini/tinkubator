package org.linkedprocess.vm;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.linkedprocess.vm.TerminateVm;
import org.linkedprocess.os.errors.VmWorkerNotFoundException;
import org.linkedprocess.Error;
import org.linkedprocess.*;
import org.linkedprocess.farm.LopFarm;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @version LoPSideD 0.1
 */
public class TerminateVmPacketListener extends VmPacketListener {

    public TerminateVmPacketListener(LopVm lopVm) {
        super(lopVm);
    }

    public void processPacket(Packet terminateVm) {

        try {
            processTerminateVmPacket((TerminateVm) terminateVm);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void processTerminateVmPacket(TerminateVm terminateVm) {
        LopFarm.LOGGER.info("Arrived " + TerminateVmPacketListener.class.getName());
        LopFarm.LOGGER.info(terminateVm.toXML());

        TerminateVm returnTerminateVm = new TerminateVm();
        returnTerminateVm.setTo(terminateVm.getFrom());
        returnTerminateVm.setFrom(this.getLopVm().getFullJid());
        returnTerminateVm.setPacketID(terminateVm.getPacketID());

        String vmPassword = terminateVm.getVmPassword();
        boolean terminate = false;

        if (null == vmPassword) {
            returnTerminateVm.setType(IQ.Type.ERROR);
            returnTerminateVm.setLopError(new Error(XMPPError.Condition.bad_request, LinkedProcess.LopErrorType.MALFORMED_PACKET, "terminate_vm XML packet is missing the vm_password attribute", LOP_CLIENT_TYPE, terminateVm.getPacketID()));
        } else if (!((LopVm) this.lopClient).checkVmPassword(vmPassword)) {
            returnTerminateVm.setType(IQ.Type.ERROR);
            returnTerminateVm.setLopError(new org.linkedprocess.Error(XMPPError.Condition.not_authorized, LinkedProcess.LopErrorType.WRONG_VM_PASSWORD, null, LOP_CLIENT_TYPE, terminateVm.getPacketID()));
        } else {
            terminate = true;
            returnTerminateVm.setType(IQ.Type.RESULT);
        }

        LopFarm.LOGGER.info("Sent " + TerminateVmPacketListener.class.getName());
        LopFarm.LOGGER.info(returnTerminateVm.toXML());
        this.getLopVm().getConnection().sendPacket(returnTerminateVm);

        if (terminate) {
            try {
                this.getLopVm().getFarm().getVmScheduler().terminateVirtualMachine(lopClient.getFullJid());
            } catch (VmWorkerNotFoundException e) {
                this.getLopVm().shutdown();
            }
        }
    }
}
