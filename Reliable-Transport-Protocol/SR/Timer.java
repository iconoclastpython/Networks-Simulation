package com.company;

/**
 * Created by Yigang on 11/4/2015.
 */
public class Timer
{
    private double sndRTT = 0;
    private double rcvRTT = 0;
    private double sndCom = 0;
    private double rcvCom = 0;
    private int seqNum = -1;

    public double getSndRTT()
    {
        return this.sndRTT;
    }

    public double getRcvRTT()
    {
        return this.rcvRTT;
    }

    public double getSndCom()
    {
        return this.sndCom;
    }

    public double getRcvCom()
    {
        return this.rcvCom;
    }

    public int getSeqNum()
    {
        return this.seqNum;
    }

    public void setSndRTT(double sr)
    {
        this.sndRTT = sr;
    }

    public void setRcvRTT(double rr)
    {
        this.rcvRTT = rr;
    }

    public void setSndCom(double sc)
    {
        this.sndCom = sc;
    }

    public void setRcvCom(double rc)
    {
        this.rcvCom = rc;
    }

    public void setSeqNum(int sn)
    {
        this.seqNum = sn;
    }
}
