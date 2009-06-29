package gov.lanl.cnls.linkedprocess.xmpp.lopvm;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

import gov.lanl.cnls.linkedprocess.os.Job;
import gov.lanl.cnls.linkedprocess.os.ServiceRefusedException;

/**
 * User: marko
 * Date: Jun 23, 2009
 * Time: 2:32:50 PM
 */
public class EvaluateListener implements PacketListener {

    private XmppVirtualMachine vm;

    public EvaluateListener(XmppVirtualMachine vm) {
        this.vm = vm;
    }

    public void processPacket(Packet eval) {

        try {
            XmppVirtualMachine.LOGGER.info("Arrived EvaluateListener:");
            XmppVirtualMachine.LOGGER.info(eval.toXML());

            try {
                String expression = ((Evaluate) eval).getExpression();
                String iqId = eval.getPacketID();
                String appJid = eval.getFrom();

                Job job = new Job(appJid, iqId, expression, vm.getFullJid());
                vm.getFarm().getScheduler().addJob(vm.getFullJid(), job);

            } catch(ServiceRefusedException e) {
                Evaluate returnEvaluate = new Evaluate();
                returnEvaluate.setTo(eval.getFrom());
                returnEvaluate.setPacketID(eval.getPacketID());
                returnEvaluate.setExpression(e.getMessage());
                returnEvaluate.setType(IQ.Type.ERROR);
                vm.getConnection().sendPacket(returnEvaluate);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
