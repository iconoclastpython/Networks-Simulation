package com.company;

/**
 * Created by Yigang on 11/12/2015.
 *
 */
public interface EventList
{
    public boolean add(Event e);
    public Event removeNext();
    public String toString();
    public double getLastPacketTime(double time, int entity);
}
