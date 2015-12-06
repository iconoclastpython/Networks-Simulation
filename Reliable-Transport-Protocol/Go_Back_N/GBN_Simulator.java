package com.company;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by Yigang on 10/29/2015.
 */
public class GBN_Simulator extends NetworkSimulator
{
    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
    int A_Application = 0;
    int A_Transition = 0;
    int B_Application = 0;
    int B_Transition = 0;

    //Current packet for each entity
    private Packet pktFromA = new Packet(0, 0, 0, "");
    private Packet pktFromB = new Packet(0, 0, 0, "");

    //For Sender
    private int base;
    private int nextSeqNum = 0;
    private Packet[] buf_wnd;

    //For Receiver
    int expectedSeqNum;


    //-----Statistics-----//
    private double start;
    private double end;
    private double totaltime;
    private ArrayList<Double> timeline = new ArrayList<Double>();

    //Total packets has been sent for each entity
    private int Asend = 0;
    private int Bsend = 0;

    //ACKed packets number
    private int ACKed = 0;

    //Variables for counting corruption packets and rate
    private int corrupt = 0;

    //Variables for counting average RTT
    private double AsendRTT = 0;
    private double ArcvRTT = 0;
    private double avgRTT = 0;
    private ArrayList<Double> RTT = new ArrayList<Double>();

    //Variables for counting average communication time for each packet
    private double AsendCom = 0;
    private double ArcvCom = 0;
    private double avgComTime = 0;
    private ArrayList<Double> avg_ComTime = new ArrayList<Double>();

    //Variables for counting average retransmission time
    private int retNum = 0;
    private double retTimer = 0;
    private ArrayList<Double> ret_Timer = new ArrayList<Double>();



    // This is the constructor.  Don't touch!
    public GBN_Simulator(int numMessages, double loss, double corrupt,
                         double avgDelay, int trace, int seed,
                         int winsize, double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize+1;
        RxmtInterval = delay;
    }


    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {
        System.out.println("aOutput start.");

        A_Application += 1;

        int checksum = 0;

        if (nextSeqNum < base + WindowSize)
        {
            //Set packet
            pktFromA.setSeqnum(nextSeqNum);
            pktFromA.setAcknum(0);
            pktFromA.setPayload(message.getData());

            checksum += nextSeqNum;
            checksum += pktFromA.getAcknum();
            for (int i = 0; i < pktFromA.getPayload().length(); i++)
                checksum += (int) pktFromA.getPayload().charAt(i);

            pktFromA.setChecksum(checksum);

            //Saving the unACKed packet in buffer
            buf_wnd[nextSeqNum % WindowSize] = pktFromA;

            //Start timer only for the first unACKed packet(base)
            if (base == nextSeqNum)
            {
                startTimer(A, RxmtInterval);
                //Mark send time
                AsendRTT = getTime();
                AsendCom = getTime();
                //start = getTime();
                //timeline.add(start);
            }

            nextSeqNum++;
            toLayer3(A, pktFromA);

            A_Transition += 1;

            //count
            Asend++;
        }

        System.out.println("aOutput end.");
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        System.out.println("aInput start.");
        int checksum;

        checksum = packet.getSeqnum();
        checksum += packet.getAcknum();
        for(int i = 0; i < packet.getPayload().length(); i++)
            checksum += (int)packet.getPayload().charAt(i);

        //Packet with ACK not corrupted
        if(packet.getChecksum() == checksum)
        {
            if(base == packet.getAcknum())
                ACKed++;

            base = packet.getAcknum() + 1;

            //Stop timer after the ACK for all unACKed packets is received
            if(packet.getAcknum() == nextSeqNum-1)
            {
                stopTimer(A);


                //Mark receive time
                ArcvRTT = getTime();
                RTT.add(ArcvRTT-AsendRTT);
                ArcvCom = getTime();
                avg_ComTime.add(ArcvCom-AsendCom);
                //end = getTime();
                //timeline.add(end);
            }
            //Restart timer if unACKed packets exist
            else
            {
                stopTimer(A);
                startTimer(A, RxmtInterval);

                //Mark retransmission time
                AsendRTT = getTime();
            }
        }
        //Packet with ACK corrupted
        else
        {
            corrupt++;
            System.out.println("A: ACK packet is corrupted.");
        }

        System.out.println("aInput end.");
    }

    // This routine will be called when A's timer expires (thus generating a
    // timer interrupt). You'll probably want to use this routine to control
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped.
    protected void aTimerInterrupt()
    {
        System.out.println("aTimerInterrupt start.");

        startTimer(A, RxmtInterval);
        //Mark retransmission time
        AsendRTT = getTime();
        retNum++;
        retTimer = getTime();
        ret_Timer.add(retTimer);

        for(int i = base; i < nextSeqNum; i++)
        {
            toLayer3(A, buf_wnd[i % WindowSize]);
            A_Transition += 1;
            Asend++;
        }

        System.out.println("aTimerInterrupt end.");
    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        System.out.println("aInit start.");

        base = 1;
        nextSeqNum = 1;
        buf_wnd = new Packet[WindowSize];

        System.out.println("aInit end.");
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        System.out.println("bInput start.");

        B_Transition += 1;

        int checksum;

        checksum = packet.getSeqnum();
        checksum += packet.getAcknum();
        for(int i = 0; i < packet.getPayload().length(); i++)
            checksum += (int)packet.getPayload().charAt(i);

        //ACK packet
        if(packet.getChecksum() == checksum && packet.getSeqnum() == expectedSeqNum)
        {
            pktFromB.setSeqnum(0);
            pktFromB.setAcknum(expectedSeqNum);
            pktFromB.setPayload("");
            pktFromB.setChecksum(expectedSeqNum);

            toLayer3(B, pktFromB);
            Bsend++;

            toLayer5(packet.getPayload());

            B_Application += 1;

            expectedSeqNum++;
        }
        //Duplicate ACK packet
        else if(packet.getChecksum() == checksum)
        {
            System.out.println("Expecting packet with sequence number " + expectedSeqNum + ". Discarding packet " + packet.getSeqnum());
            pktFromB.setSeqnum(0);
            pktFromB.setAcknum(expectedSeqNum-1);
            pktFromB.setPayload("");
            pktFromB.setChecksum(expectedSeqNum-1);

            //Send duplicate ACK to A
            toLayer3(B, pktFromB);
            Bsend++;
        }
        else
        {
            corrupt++;
            System.out.println("B: Corrupted packet received");
        }


        System.out.println("bInput end.");
    }

    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        System.out.println("bInit start.");

        expectedSeqNum = 1;

        System.out.println("bInit end.");
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
        //Calculate total time
        totaltime = getTime();

        //Calculate average RTT
        double sumRTT = 0;
        for(double e : RTT)
            sumRTT += e;
        avgRTT = sumRTT / RTT.size();

        //Calculate average communication time
        double sumComTime = 0;
        for(double e : avg_ComTime)
            sumComTime += e;
        avgComTime = sumComTime / avg_ComTime.size();

        System.out.println();
        System.out.println("Simulation_done.");
        System.out.println("Protocol: GBN result:");
        System.out.println(A_Application + " packets sent from application layer of the sender.");
        System.out.println(A_Transition + " packets sent from the transport layer of the sender.");
        System.out.println(B_Transition + " packets received at the transport layer of receiver.");
        System.out.println(B_Application + " packets received at the application layer of the receiver.");

        System.out.println("Total time cost: " + totaltime);
        System.out.println("Total packet sent from A and B: " + Asend+Bsend);

        System.out.println("Average RTT: " + avgRTT);
        System.out.println("Average communication time: " + avgComTime);
        //System.out.println("RTT line: " + RTT);
        //System.out.println("Communication time: " + avg_ComTime);

        //System.out.println("ACKed packets: " + ACKed);
        System.out.println("Retransmission time: " + retNum);
        //System.out.println("Retransmission time point are: " + ret_Timer);

        System.out.println("Corrupted packet number: " + corrupt);
        System.out.println("Throughput: " + B_Transition / totaltime);
        System.out.println("Goodput: " + B_Application / totaltime);
    }
}
