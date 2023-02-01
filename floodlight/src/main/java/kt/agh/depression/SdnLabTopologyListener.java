package kt.agh.depression;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.topology.ITopologyListener;
import net.juniper.netconf.Device;

import net.juniper.netconf.CommitException;
import net.juniper.netconf.LoadException;
import net.juniper.netconf.NetconfException;
import net.juniper.netconf.XML;
import net.juniper.netconf.XMLBuilder;

public class SdnLabTopologyListener implements ITopologyListener {
	protected static final Logger logger = LoggerFactory
			.getLogger(SdnLabTopologyListener.class);

	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		logger.debug("Received topology status");
		for (ILinkDiscovery.LDUpdate update : linkUpdates) {
			switch (update.getOperation()) {
			case LINK_UPDATED:
				logger.debug("Link updated. Update {}", update.toString());
				break;
			case LINK_REMOVED:
				logger.debug("Link removed. Update {}", update.toString());
				break;
			case SWITCH_UPDATED:
				// logger.debug("Switch updated. Update {}", update.toString());

				try {
					connect_netconf();
				} catch (NetconfException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				break;
			case SWITCH_REMOVED:
				logger.debug("Switch removed. Update {}", update.toString());
				break;
			default:
				break;
			}
		}
	}

	// ////////////////

	public void connect_netconf() throws NetconfException,
			ParserConfigurationException, SAXException, IOException {

		// Create device
		Device device = new Device("10.80.0.42", "lab", "mxlab17", null, 830);
		device.connect();
		XMLBuilder builder = new XMLBuilder();
		XML ftp_config = builder.createNewConfig("system", "services", "ftp");

		
		boolean isLocked = device.lockConfig();
        if(!isLocked) {
            System.out.println("Could not lock configuration. Exit now.");
            return;
        }
        
        try {
            device.loadXMLConfiguration(ftp_config.toString(), "merge");
            device.commit();
        } catch(LoadException | CommitException e) {
            System.out.println(e.getMessage());
            return;
        }
        
        device.unlockConfig();
        
		// boolean status = device.isOK();

		// Send RPC and receive RPC Reply as XML
		// XML rpc_reply = device.executeRPC("get-config");
		/*
		 * OR device.executeRPC("<get-interface-information/>"); OR
		 * device.executeRPC("<rpc><get-interface-information/></rpc>");
		 */
		// Print the RPC-Reply and close the device.
		// System.out.println(rpc_reply);
		logger.info("?????????????????????????????????????????????????????");
		// logger.info("device status {}", rpc_reply);
		logger.info("config {}", ftp_config);
		device.close();
	}

}