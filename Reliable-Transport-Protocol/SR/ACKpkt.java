package com.company;

/**
 * Created by Yigang on 11/3/2015.
 */
public class ACKpkt
{
    private int seqNum = -1;
    private boolean acked = false;

    public ACKpkt(){}

    public ACKpkt(int s, boolean a, double delay, double rtt, double e)
    {
        this.seqNum = s;
        this.acked = a;
    }

    public int getSeqNum()
        {
            return this.seqNum;
        }

    public boolean getAck()
        {
            return this.acked;
        }

    public void setSeqNum(int s)
        {
            this.seqNum = s;
        }

    public void setAck(boolean a)
        {
            this.acked = a;
        }

}
