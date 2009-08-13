package org.linkedprocess.gui.villein;

import org.linkedprocess.Connection;
import org.linkedprocess.gui.villein.vmcontrol.VmControlFrame;
import org.linkedprocess.xmpp.villein.proxies.VmProxy;
import org.linkedprocess.xmpp.villein.XmppVillein;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * User: marko
 * Date: Jul 7, 2009
 * Time: 10:33:02 PM
 */
public class VilleinGui extends JFrame {

    protected static final String FRAME_TITLE = "LoPSideD Villein";

    protected XmppVillein xmppVillein;
    protected CountrysideArea countrysideArea;
    protected Map<String, VmControlFrame> vmFrames = new HashMap<String, VmControlFrame>();

    public VilleinGui() {
        super(VilleinGui.FRAME_TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.loadLoginFrame();
    }

    public Connection getConnection() {
        return this.xmppVillein.getConnection();
    }

    public void loadLoginFrame() {
        this.getContentPane().removeAll();
        this.getContentPane().add(new VilleinLoginArea(this));
        this.setResizable(false);
        this.pack();
        this.setVisible(true);
    }

    public void loadHostArea(XmppVillein xmppVillein) {
        this.xmppVillein = xmppVillein;

        this.getContentPane().removeAll();
        this.countrysideArea = new CountrysideArea(this);
        this.countrysideArea.createTree();
        this.xmppVillein.addPresenceHandler(this.countrysideArea);
        this.getContentPane().add(countrysideArea);
        this.setResizable(true);
        this.pack();
        this.setVisible(true);

    }

    public void addVmFrame(VmProxy vmProxy) {
        VmControlFrame vmControlFrame = new VmControlFrame(vmProxy, this);
        this.vmFrames.put(vmProxy.getFullJid(), vmControlFrame);
    }

    public void removeVmFrame(VmProxy vmProxy) {
        VmControlFrame vmControlFrame = this.vmFrames.remove(vmProxy.getFullJid());
        if (vmControlFrame != null) {
            vmControlFrame.setVisible(false);
        }
    }

    public VmControlFrame getVmFrame(String vmJid) {
        return this.vmFrames.get(vmJid);
    }

    public void updateHostAreaTree(String jid, boolean remove) {
        this.countrysideArea.updateTree(jid, remove);
    }

    public void createTree() {
        this.countrysideArea.createTree();
    }

    public XmppVillein getXmppVillein() {
        return this.xmppVillein;
    }

    public CountrysideArea getHostArea() {
        return this.countrysideArea;
    }

    public void shutDown() {
        if (this.xmppVillein != null)
            this.xmppVillein.shutDown(null);
        System.exit(0);
    }

    public static void main(String[] args) {
        new VilleinGui();
    }
}
