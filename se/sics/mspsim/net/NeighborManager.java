package se.sics.mspsim.net;
import java.util.TimerTask;
public class NeighborManager extends TimerTask {

    private NeighborTable neigborTable;
    private IPStack ipStack;
    private long nextRS = 0;
    private boolean duplicateDetectionNS;

    public NeighborManager(IPStack stack, NeighborTable table) {
      neigborTable = table;
      ipStack = stack;
      stack.getTimer().schedule(this, 1000, 1000);
    }

    public void run() {
      long time = System.currentTimeMillis();
      if (!duplicateDetectionNS) {
        /* send a duplicate detection message */
        System.out.println("NeighborManager: sending neighbor solicitation (DAD)");
        duplicateDetectionNS = true;
        ICMP6Packet icmp = new ICMP6Packet(ICMP6Packet.NEIGHBOR_SOLICITATION);
        icmp.targetAddress = ipStack.myLinkAddress;
        IPv6Packet ipp = new IPv6Packet(icmp, ipStack.myLocalIPAddress, ipStack.myLocalSolicited);
        ipStack.sendPacket(ipp, null);
      } else if (!ipStack.isRouter() && neigborTable.getDefrouter() == null && nextRS < time) {
        System.out.println("NeighborManager: sending router solicitation");
        nextRS = time + 10000;
        ICMP6Packet icmp = new ICMP6Packet(ICMP6Packet.ROUTER_SOLICITATION);
        icmp.addLinkOption(ICMP6Packet.SOURCE_LINKADDR,
            ipStack.getLinkLayerAddress());
        IPv6Packet ipp = new IPv6Packet(icmp, ipStack.myLocalIPAddress, IPStack.ALL_ROUTERS);
        ipStack.sendPacket(ipp, null);
      }
    }
    
    public void receiveNDMessage(IPv6Packet packet) {
      /* payload is a ICMP6 packet */
      ICMP6Packet payload = (ICMP6Packet) packet.getIPPayload();
      Neighbor nei = null;
      switch (payload.type) {
      case ICMP6Packet.ROUTER_SOLICITATION:
        nei = neigborTable.addNeighbor(packet.sourceAddress, packet.getLinkSource());
        if (nei != null) {
          nei.setState(Neighbor.REACHABLE);
        }
        
        if (ipStack.isRouter()) {
            ICMP6Packet p = new ICMP6Packet();
            p.targetAddress = payload.targetAddress;
            p.type = ICMP6Packet.ROUTER_ADVERTISEMENT;
            p.flags = ICMP6Packet.FLAG_SOLICITED |
            ICMP6Packet.FLAG_OVERRIDE;

            /* ensure that the RA is updated... */
            p.updateRA(ipStack);

            IPv6Packet ipp = new IPv6Packet();
            ipp.setIPPayload(p);
            // is this ok?
            //ipp.destAddress = packet.sourceAddress;
            ipp.destAddress = IPStack.ALL_NODES; //packet.sourceAddress;
            ipp.sourceAddress = ipStack.myLocalIPAddress;
            System.out.print("Created ICMP6 RA for ");
            IPv6Packet.printAddress(System.out, ipp.destAddress);
            packet.printPacket(System.out);

            ipStack.sendPacket(ipp, packet.netInterface);
        }
        break;
      case ICMP6Packet.ROUTER_ADVERTISEMENT:
        nei = neigborTable.addNeighbor(packet.sourceAddress, packet.getLinkSource());
        neigborTable.setDefrouter(nei);
        nei.setState(Neighbor.REACHABLE);
        break;
      }

    }
}