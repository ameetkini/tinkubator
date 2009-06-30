package gov.lanl.cnls.linkedprocess.xmpp.lopvm;

import java.util.logging.Logger;

import gov.lanl.cnls.linkedprocess.LinkedProcess;
import gov.lanl.cnls.linkedprocess.os.ServiceRefusedException;
import gov.lanl.cnls.linkedprocess.os.Job;
import gov.lanl.cnls.linkedprocess.xmpp.XmppClient;
import gov.lanl.cnls.linkedprocess.xmpp.lopfarm.XmppFarm;


import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;

/**
 * User: marko
 * Date: Jun 23, 2009
 * Time: 11:01:06 AM
 */
public class XmppVirtualMachine extends XmppClient {

    public static Logger LOGGER = LinkedProcess.getLogger(XmppVirtualMachine.class);
    public static String RESOURCE_PREFIX = "LoPVM";

    public static enum VirtualMachinePresence {
        AVAILABLE, TOO_MANY_JOBS
    }

    protected XmppFarm farm;

    public XmppVirtualMachine(final String server, final int port, final String username, final String password, XmppFarm farm) {

        this.farm = farm;

        LOGGER.info("Starting LoP virtual machine");
        // Registering the types of IQ packets/stanzas the the Lop VM can respond to.
        ProviderManager pm = ProviderManager.getInstance();
        pm.addIQProvider(LinkedProcess.EVALUATE_TAG, LinkedProcess.LOP_VM_NAMESPACE, new EvaluateProvider());
        pm.addIQProvider(LinkedProcess.JOB_STATUS_TAG, LinkedProcess.LOP_VM_NAMESPACE, new JobStatusProvider());
        pm.addIQProvider(LinkedProcess.ABORT_JOB_TAG, LinkedProcess.LOP_VM_NAMESPACE, new AbortJobProvider());

        try {
            this.logon(server, port, username, password);
            this.initiateFeatures();
            //this.printClientStatistics();
        } catch (XMPPException e) {
            LOGGER.severe("error: " + e);
            System.exit(1);
        }

        PacketFilter evalFilter = new AndFilter(new PacketTypeFilter(Evaluate.class), new IQTypeFilter(IQ.Type.GET));
        PacketFilter statusFilter = new AndFilter(new PacketTypeFilter(JobStatus.class), new IQTypeFilter(IQ.Type.GET));
        PacketFilter abandonFilter = new AndFilter(new PacketTypeFilter(AbortJob.class), new IQTypeFilter(IQ.Type.GET));

        connection.addPacketListener(new EvaluateListener(this), evalFilter);
        connection.addPacketListener(new JobStatusListener(this), statusFilter);
        connection.addPacketListener(new AbortJobListener(this), abandonFilter);
    }

    protected void logon(String server, int port, String username, String password) throws XMPPException {

        super.logon(server, port, username, password, RESOURCE_PREFIX);
        connection.sendPacket(this.createVMPresence(VirtualMachinePresence.AVAILABLE));
    }

    public XmppFarm getFarm() {
        return this.farm;
    }

    public final Presence createVMPresence(final VirtualMachinePresence type) {
        String statusMessage = "temp util engine enacted.";
        //String statusMessage = engine.getFactory().getLanguageName() + "(" + engine.getFactory().getLanguageVersion() + "):" + engine.getFactory().getEngineName() + "(" + engine.getFactory().getEngineVersion() + ")";
        if (type == VirtualMachinePresence.AVAILABLE) {
            return new Presence(Presence.Type.available, statusMessage, LinkedProcess.LOWEST_PRIORITY, Presence.Mode.available);
        } else {
            return new Presence(Presence.Type.unavailable, statusMessage, LinkedProcess.LOWEST_PRIORITY, Presence.Mode.dnd);
        }
    }

    public void abortJob(String jobId) throws ServiceRefusedException {
        this.farm.getScheduler().abortJob(this.getFullJid(), jobId);
    }

    public LinkedProcess.JobStatus getJobStatus(String jobId) {
        return this.farm.getScheduler().getJobStatus(this.getFullJid(), jobId);
    }

    public void scheduleJob(Job job) throws ServiceRefusedException {
        this.farm.getScheduler().scheduleJob(this.getFullJid(), job);

    }

    protected void initiateFeatures() {
        super.initiateFeatures();
        discoManager.addFeature(LinkedProcess.LOP_VM_NAMESPACE);
    }

}
