package se.sics.mspsim.net;

public class ICMP6PacketHandler {

  IPStack ipStack;

  public ICMP6PacketHandler(IPStack stack) {
    ipStack = stack;
  }

  public void handlePacket(IPv6Packet packet) {
    ICMP6Packet icmpPacket = new ICMP6Packet();
    icmpPacket.parsePacketData(packet);

    icmpPacket.printPacket(System.out);

    /* handle packet - just a test for now */
    ICMP6Packet p;
    IPv6Packet ipp;
    switch (icmpPacket.type) {
    case ICMP6Packet.ECHO_REQUEST:
      p = new ICMP6Packet();
      p.type = ICMP6Packet.ECHO_REPLY;
      p.seqNo = icmpPacket.seqNo;
      p.id = icmpPacket.id;

      ipp = new IPv6Packet();
      ipp.setIPPayload(p);
      // is this ok?
      ipp.destAddress = packet.sourceAddress;
      ipp.sourceAddress = ipStack.myLocalIPAddress;
      ipStack.sendPacket(ipp, packet.netInterface);
      break;
    case ICMP6Packet.ECHO_REPLY:
      System.out.println("ICMP6 got echo reply!!");
      break;      
    case ICMP6Packet.NEIGHBOR_SOLICITATION:
      p = new ICMP6Packet();
      p.targetAddress = icmpPacket.targetAddress;
      p.type = ICMP6Packet.NEIGHBOR_ADVERTISEMENT;      
      p.flags = ICMP6Packet.FLAG_SOLICITED |
      ICMP6Packet.FLAG_OVERRIDE;
      if (ipStack.isRouter()) {
        p.flags |= ICMP6Packet.FLAG_ROUTER;
      }
        /* always send the linkaddr option */
      p.addLinkOption(ICMP6Packet.TARGET_LINKADDR, ipStack.getLinkLayerAddress());
      ipp = new IPv6Packet();
      ipp.setIPPayload(p);
      // is this ok?
      ipp.destAddress = packet.sourceAddress;
      ipp.sourceAddress = ipStack.myLocalIPAddress;
      ipStack.sendPacket(ipp, packet.netInterface);
      break;
    case ICMP6Packet.ROUTER_SOLICITATION:
      if (ipStack.isRouter()) {
        p = new ICMP6Packet();
        p.targetAddress = icmpPacket.targetAddress;
        p.type = ICMP6Packet.ROUTER_ADVERTISEMENT;
        p.flags = ICMP6Packet.FLAG_SOLICITED |
        ICMP6Packet.FLAG_OVERRIDE;

        /* ensure that the RA is updated... */
        p.updateRA(ipStack);

        ipp = new IPv6Packet();
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
    }
  }
}