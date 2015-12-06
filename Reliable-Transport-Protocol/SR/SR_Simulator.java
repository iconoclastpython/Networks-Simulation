package com.company;

import java.util.ArrayList;

/**
 * Created by Yigang on 11/1/2015.
 */
public class SR_Simulator extends NetworkSimulator
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
    private Packet pktFromA;
    private Packet pktFromB;

    //For Sender
    private int head, tail;
    private int snd_base;
    private int nextSeqNum;
    private ACKpkt[] Sender_Window;
    private ArrayList<Packet> send_buffer;
    private int timeoutCount;

    //For Receiver
    private int rcv_base;
    private int expectedSeqNum;
    private int lastRcvSeqNum;
    private int bufferSize;
    private boolean outoforder;
    private Packet[] rcv_buffer;


    //-----------------------Statistics---------------------------//
    private double totaltime;
    private ArrayList<Double> timeline = new ArrayList<Double>();

    //Total packets has been sent for each entity
    private int Asend = 0;
    private int Bsend = 0;

    //ACKed packets number
    private int ACKed = 0;

    //Variables for counting loss and corruption packets and rate
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

    //Timer
    //private  timer;
    private ArrayList<Timer> timerLine = new ArrayList<>();



    // This is the constructor.  Don't touch!
    public SR_Simulator(int numMessages, double loss, double corrupt,
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

        A_Application++;
        int checksum = 0;

        //Set packet
        pktFromA.setSeqnum(nextSeqNum);
        pktFromA.setAcknum(0);
        pktFromA.setPayload(message.getData());

        checksum += nextSeqNum;
        checksum += pktFromA.getAcknum();
        for (int i = 0; i < pktFromA.getPayload().length(); i++)
            checksum += (int) pktFromA.getPayload().charAt(i);

        pktFromA.setChecksum(checksum);

        tail++;
        send_buffer.add(pktFromA);

        int temp = nextSeqNum-snd_base+head;

        while(nextSeqNum < snd_base + WindowSize && temp != tail+1)
        {
            System.out.println("temp: " + temp);
            Sender_Window[(nextSeqNum-1) % WindowSize].setSeqNum((send_buffer.get(temp)).getSeqnum());
            Sender_Window[(nextSeqNum-1) % WindowSize].setAck(false);
            toLayer3(A, send_buffer.get(temp));

            A_Transition++;

            if(snd_base == nextSeqNum)
            {
                startTimer(A, RxmtInterval);
                Timer timer = new Timer();
                timerLine.add(timer);
                timer.setSndRTT(getTime());
                timer.setSndCom(getTime());
                timer.setSeqNum(nextSeqNum);
            }

            nextSeqNum++;
            temp++;
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

        if(packet.getChecksum() == checksum)
        {
            if (packet.getAcknum() == snd_base)
            {
                int iter = snd_base + 1;
                timeoutCount = 0;
                head++;

                while (Sender_Window[(iter - 1) % WindowSize].getSeqNum() != -1 && Sender_Window[(iter - 1) % WindowSize].getAck() == true)
                {
                    Sender_Window[(iter - 1) % WindowSize].setSeqNum(-1);
                    Sender_Window[(iter - 1) % WindowSize].setAck(false);
                    iter++;
                    head++;
                }
                snd_base = iter;

                if (snd_base == nextSeqNum)
                {
                    stopTimer(A);

                    ACKed++;

                    for(Timer e : timerLine)
                    {
                        if(e.getSeqNum() == packet.getAcknum())
                        {
                            e.setRcvCom(getTime());
                            e.setRcvRTT(getTime());
                        }
                    }
                }
                else
                {
                    stopTimer(A);
                    startTimer(A, RxmtInterval);
                }
            }
            else if(packet.getAcknum() > snd_base && packet.getAcknum() < nextSeqNum)
            {
                Sender_Window[(packet.getAcknum() - 1) % WindowSize].setSeqNum(packet.getAcknum());
                Sender_Window[(packet.getAcknum() - 1) % WindowSize].setAck(true);
            }
        }
        else
        {
            corrupt++;
            System.out.println("A: Corrupt packet received.");
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

        A_Transition++;

        toLayer3(A, send_buffer.get(head));
        startTimer(A, RxmtInterval);

        for(Timer e : timerLine)
        {
            if(e.getSeqNum() == send_buffer.get(head).getSeqnum())
            {
                e.setSndRTT(getTime());
            }
        }

        timeoutCount++;

        int temp = head + 1;
        for(int i = 1; i < timeoutCount && i < WindowSize; i++)
        {
            //New line maybe not correct to work
            if(temp == send_buffer.size())
                break;

            if(Sender_Window[(snd_base+i-1) % WindowSize].getSeqNum() != -1 && Sender_Window[(snd_base+i-1) % WindowSize].getAck() == false)
            {
                toLayer3(A, send_buffer.get(temp));

                retNum++;

                A_Transition++;

                for(Timer e : timerLine)
                {
                    if(e.getSeqNum() == send_buffer.get(temp).getSeqnum())
                    {
                        e.setSndRTT(getTime());
                    }
                }
            }
            temp++;
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

        head = FirstSeqNo;
        tail = -1;
        snd_base = 1;
        nextSeqNum = 1;
        send_buffer = new ArrayList<>();
        pktFromA = new Packet(0, 0, 0, "");

        Sender_Window = new ACKpkt[WindowSize];
        for(int i = 0; i < WindowSize; i++)
            Sender_Window[i] = new ACKpkt();

        System.out.println("aInit end.");
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        System.out.println("bInput start.");

        B_Transition++;

        int checksum;
        checksum = packet.getSeqnum();
        checksum += packet.getAcknum();
        for(int i = 0; i < packet.getPayload().length(); i++)
            checksum += (int)packet.getPayload().charAt(i);

        if(packet.getChecksum() != checksum)
        {
            corrupt++;
            System.out.println("B: Corrupt packet received.");
            return;
        }


        if(packet.getSeqnum() == rcv_base)
        {
            toLayer5(packet.getPayload());

            B_Application++;

            int iter = rcv_base+1, rcv_base_increase = 1;
            while(rcv_buffer[(iter-1) % WindowSize].getSeqnum() != -1)
            {
                toLayer5(rcv_buffer[(iter-1) % WindowSize].getPayload());

                B_Application++;

                rcv_base_increase++;
                rcv_buffer[(iter-1) % WindowSize].setSeqnum(-1);
                iter++;
            }
            rcv_base += rcv_base_increase;
        }
        else if(packet.getSeqnum() > rcv_base && packet.getSeqnum() < rcv_base+WindowSize)
        {
            int index = (packet.getSeqnum()-1) % WindowSize;

            if(rcv_buffer[index].getSeqnum() != packet.getSeqnum())
                rcv_buffer[index] = packet;
        }
        else if(packet.getSeqnum() >= rcv_base-WindowSize && packet.getSeqnum() < rcv_base)
        {
            System.out.println("B: Drop this received packet for out of window.");
        }
        else
        {
            System.out.println("B: Invalid packet received.");
            return;
        }

        pktFromB.setSeqnum(0);
        pktFromB.setAcknum(packet.getSeqnum());
        pktFromB.setPayload("");
        pktFromB.setChecksum(packet.getSeqnum());

        toLayer3(B, pktFromB);

        System.out.println("bInput end.");
    }

    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        System.out.println("bInit start.");

        rcv_base = 1;
        pktFromB = new Packet(0, 0, 0, "");
        rcv_buffer = new Packet[WindowSize];
        for(int i = 0; i < WindowSize; i++)
            rcv_buffer[i] = new Packet(-1, 0, 0, "");

        System.out.println("bInit end.");
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
        totaltime = getTime();

        double sumRtt = 0;
        double sumCom = 0;

        for(Timer e : timerLine)
        {
            if(e.getRcvCom() != 0 && e.getRcvRTT() != 0)
            {
                double rtt = e.getRcvRTT() - e.getSndRTT();
                double com = e.getRcvCom() - e.getSndCom();
                sumRtt += rtt;
                sumCom += com;
            }
        }
        avgRTT = sumRtt / timerLine.size();
        avgComTime = sumCom / timerLine.size();

        System.out.println();
        System.out.println("Simulation_done.");
        System.out.println("Protocol: SR result:");

        System.out.println(A_Application + " packets sent from application layer of the sender.");
        System.out.println(A_Transition + " packets sent from the transport layer of the sender.");
        System.out.println(B_Transition + " packets received at the transport layer of receiver.");
        System.out.println(B_Application + " packets received at the application layer of the receiver.");

        System.out.println("Total time cost: " + totaltime);

        System.out.println("Average RTT: " + avgRTT);
        System.out.println("Average communication time: " + avgComTime);

        //System.out.println("ACKed packets: " + ACKed);

        System.out.println("Retransmission time: " + retNum);
        System.out.println("Corrupt packet number: " + corrupt);

        System.out.println("Throughput: " + B_Transition / totaltime);
        System.out.println("Goodput: " + B_Application / totaltime);

    }
}
