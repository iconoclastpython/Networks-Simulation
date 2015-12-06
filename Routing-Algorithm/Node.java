package com.company;

/**
 * Created by Yigang on 11/12/2015.
 *
 */

/**
 * This is the class that students need to implement. The code skeleton is provided.
 * Students need to implement rtinit(), rtupdate() and linkhandler().
 * printdt() is provided to pretty print a table of the current costs for reaching
 * other nodes in the network.
 */

public class Node
{
    public static final int INFINITY = 9999;

    int[] lkcost;		/*The link cost between node 0 and other nodes*/
    int[][] costs;  		/*Define distance table*/
    int nodename;               /*Name of this node*/

    /* Class constructor */
    public Node() { }



    /**
     * Define all the helper functions
     *
     * tolayer2_helper: Help to make a new packet and send it to layer 2
     * printInit: Help to print the result of initialization mode
     * printUpdate: Help to print the result of update mode
     * printLinkChange: Help to print the result of link change mode
     */
    void tolayer2_helper(int source, int dest, int[] lk)
    {
        Packet sndPkt = new Packet(source, dest, lk);
        NetworkSimulator.tolayer2(sndPkt);
    }

    void printInit(int nodename)
    {
        System.out.println("Initialization Time: " + NetworkSimulator.clocktime + ".");
        System.out.println("Entity " + nodename + " INITIALIZATION COMPLETE. Distance table in current node: ");
        printdt();
        System.out.println("[-----------------------------------------]");
    }

    void printUpdate(int nodename)
    {
        System.out.println("Update Time: " + NetworkSimulator.clocktime + ".");
        System.out.println("Entity " + nodename + " UPDATED COMPLETE. Distance table in current node: ");
        printdt();
        System.out.println("[-----------------------------------------]");
    }

    void printLinkChange(int nodename)
    {
        System.out.println("Link Change Time: " + NetworkSimulator.clocktime + ". HANDLE COMPLETE!");
        System.out.println("Entity " + nodename + " link cost change has been handled. Distance table in current node: ");
        printdt();
        System.out.println("[-----------------------------------------]");
    }



    /**
     * The main engine for basic distributed BF Algorithm
     *
     * rtinit()
     * rtupdate()
     * linkhandler()
     */
    /* students to write the following two routines, and maybe some others */
    void rtinit(int nodename, int[] initial_lkcost)
    {
        /**
         * Assign the node name to field
         */
        this.nodename = nodename;

        /**
         * Initialize the minimum cost distance table to direct reachable node
         */
        lkcost = initial_lkcost;

        /**
         * Declare distance table with 4 * 4 matrix
         */
        costs = new int[4][4];

        /**
         * Initialize the whole distance table as Infinity for current node
         */
        for(int i = 0; i < 4; i++)
            for(int j = 0; j < 4; j++)
                costs[i][j] = INFINITY;

        switch(nodename)
        {
            /**
             * Initialize node n0:
             */
            case 0:
                /**
                 * Initialize the distance table for current node with its direct reachable node
                 */
                for(int i = 0; i < 4; i++)
                    costs[i][i] = initial_lkcost[i];

                /**
                 * Make packet and send to layer 2
                 */
                for(int l = 1; l < 4; l++)
                    tolayer2_helper(0, l, lkcost);

                /**
                 * Print necessary information within current node
                 */
                printInit(0);
                break;

            /******************************************************
             * The following code keeps the same comment as above *
             ******************************************************/

            /**
             * Initialize node n1:
             */
            case 1:
                for(int i = 0; i < 4; i++)
                    if(i != 3)
                        costs[i][i] = initial_lkcost[i];

                tolayer2_helper(1, 0, lkcost);
                tolayer2_helper(1, 2, lkcost);

                printInit(1);
                break;

            /**
             * Initialize node n2:
             */
            case 2:
                for(int i = 0; i < 4; i++)
                    costs[i][i] = initial_lkcost[i];

                tolayer2_helper(2, 0, lkcost);
                tolayer2_helper(2, 1, lkcost);
                tolayer2_helper(2, 3, lkcost);

                printInit(2);
                break;

            /**
             * Initialize node n3:
             */
            case 3:
                for(int i = 0; i < 4; i++)
                    if(i != 1)
                        costs[i][i] = initial_lkcost[i];

                tolayer2_helper(3, 0, lkcost);
                tolayer2_helper(3, 2, lkcost);

                printInit(3);
                break;
        }

    }

    void rtupdate(Packet rcvdpkt)
    {
        boolean isTableChanged = false;

        lkcost = new int[4];

        switch(rcvdpkt.destid)
        {
            /**
             * Update node n0:
             */
            case 0:
                /**
                 * Re-initialize the direct reachable node distance vector using current distance table
                 */
                for(int i = 0; i < 4; i++)
                    lkcost[i] = Math.min(costs[i][1], Math.min(costs[i][2], costs[i][3]));

                /**
                 * Implement Bellman-Ford equation below:
                 * d(x->y) = min{c(x, v) + d(v->y)}
                 */
                for(int j = 0; j < 4; j++)
                {
                    /**
                     * This equation calculates the minimum cost from current node to destination,
                     * may or may not via the previous node, depends on the cost.
                     *
                     * Helpful declaration:
                     * j: The index for destination from current node
                     * costs[j][rcvdpkt.sourceid]: The cost from current node to destination j via rcvdpkt.sourceid
                     * rcvdpkt.mincost[j]: The costs from rcvdpkt.sourceid to destination j
                     * lkcost[rcvdpkt.sourceid]: The cost from current node to rcvdpkt.sourceid
                     */
                    costs[j][rcvdpkt.sourceid] = Math.min(costs[j][rcvdpkt.sourceid],
                            rcvdpkt.mincost[j] + lkcost[rcvdpkt.sourceid]);

                    /**
                     * Update the new shorter cost to the distance vector of current node
                     * Assign isTableChange as true to handle the tolayer2 event
                     */
                    if(costs[j][rcvdpkt.sourceid] < lkcost[j])
                    {
                        lkcost[j] = costs[j][rcvdpkt.sourceid];
                        isTableChanged = true;
                    }
                }

                /**
                 * If the shortest path distance has changed,
                 * which means a new shorter path has been found.
                 * Send the new packet to layer 2 by new path
                 */
                if(isTableChanged)
                    for(int k = 1; k < 4; k++)
                        tolayer2_helper(0, k, lkcost);

                /**
                 * Print the updated information
                 */
                printUpdate(0);
                break;

            /******************************************************
             * The following code keeps the same comment as above *
             ******************************************************/

            /**
             * Update node n1:
             */
            case 1:
                for(int i = 0; i < 4; i++)
                    lkcost[i] =  Math.min(costs[i][0], Math.min(costs[i][2], costs[i][3]));

                for(int j = 0; j < 4; j++)
                {
                    costs[j][rcvdpkt.sourceid] = Math.min(costs[j][rcvdpkt.sourceid],
                            rcvdpkt.mincost[j] + lkcost[rcvdpkt.sourceid]);

                    if(costs[j][rcvdpkt.sourceid] < lkcost[j])
                    {
                        lkcost[j] = costs[j][rcvdpkt.sourceid];
                        isTableChanged = true;
                    }
                }

                if(isTableChanged)
                {
                    tolayer2_helper(1, 0, lkcost);
                    tolayer2_helper(1, 2, lkcost);
                }

                printUpdate(1);
                break;

            /**
             * Update node n2:
             */
            case 2:
                for(int i = 0; i < 4; i++)
                    lkcost[i] = Math.min(Math.min(costs[i][0], costs[i][1]), costs[i][3]);

                for(int j = 0; j < 4; j++)
                {
                    costs[j][rcvdpkt.sourceid] = Math.min(costs[j][rcvdpkt.sourceid],
                            rcvdpkt.mincost[j] + lkcost[rcvdpkt.sourceid]);

                    if(costs[j][rcvdpkt.sourceid] < lkcost[j])
                    {
                        lkcost[j] = costs[j][rcvdpkt.sourceid];
                        isTableChanged = true;
                    }
                }

                if(isTableChanged)
                {
                    tolayer2_helper(2, 0, lkcost);
                    tolayer2_helper(2, 1, lkcost);
                    tolayer2_helper(2, 3, lkcost);
                }

                printUpdate(2);
                break;

            /**
             * Update node n3:
             */
            case 3:
                for(int i = 0; i < 4; i++)
                    lkcost[i] =  Math.min(costs[i][2], Math.min(costs[i][0], costs[i][1]));

                for(int j = 0; j < 4; j++)
                {
                    costs[j][rcvdpkt.sourceid] = Math.min(costs[j][rcvdpkt.sourceid],
                            rcvdpkt.mincost[j] + lkcost[rcvdpkt.sourceid]);

                    if(costs[j][rcvdpkt.sourceid] < lkcost[j])
                    {
                        lkcost[j] = costs[j][rcvdpkt.sourceid];
                        isTableChanged = true;
                    }
                }

                if(isTableChanged)
                {
                    tolayer2_helper(3, 0, lkcost);
                    tolayer2_helper(3, 2, lkcost);
                }

                printUpdate(3);
                break;
        }
    }


    /* called when cost from the node to linkid changes from current value to newcost*/
    void linkhandler(int linkid, int newcost)
    {
        /**
         * Re-define distance table and reachable distance
         */
        costs = new int[4][4];
        lkcost = new int[4];

        for(int i = 0; i < 4; i++)
            for(int j = 0; j < 4; j++)
                costs[i][j] = INFINITY;

        switch(linkid)
        {
            /**
             * Update node n0:
             */
            case 1:
                costs[0][0] = 0;
                costs[1][1] = 1;
                costs[2][2] = 10;
                costs[3][3] = 7;

                /**
                 * Change the specific link costs
                 */
                costs[linkid][linkid] = newcost;

                /**
                 * Initialize the minimum distance for direct reachable node
                 */
                for(int k = 0; k < 4; k++)
                    lkcost[k] = Math.min(Math.min(costs[k][2], costs[k][3]), costs[k][1]);

                /**
                 * Send to layer 2
                 */
                for(int l = 1; l < 4; l++)
                    tolayer2_helper(0, l, lkcost);

                /**
                 * Print link change information
                 */
                printLinkChange(0);
                break;

            /******************************************************
             * The following code keeps the same comment as above *
             ******************************************************/

            /**
             * Update node n1:
             */
            case 0:
                costs[0][0] = 1;
                costs[1][1] = 0;
                costs[2][2] = 1;

                costs[linkid][linkid] = newcost;

                for(int k = 0; k < 4; k++)
                    lkcost[k] = Math.min(Math.min(costs[k][2], costs[k][3]), costs[k][0]);

                tolayer2_helper(1, 0, lkcost);
                tolayer2_helper(1, 2, lkcost);

                printLinkChange(1);
                break;
        }
    }


    /* Prints the current costs to reaching other nodes in the network */
    void printdt() {
        switch(nodename) {
            case 0:
                System.out.printf("                via     \n");
                System.out.printf("   D0 |    1     2    3 \n");
                System.out.printf("  ----|-----------------\n");
                System.out.printf("     1|  %3d   %3d   %3d\n",costs[1][1], costs[1][2],costs[1][3]);
                System.out.printf("dest 2|  %3d   %3d   %3d\n",costs[2][1], costs[2][2],costs[2][3]);
                System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][1], costs[3][2],costs[3][3]);
                break;
            case 1:
                System.out.printf("                via     \n");
                System.out.printf("   D1 |    0     2 \n");
                System.out.printf("  ----|-----------------\n");
                System.out.printf("     0|  %3d   %3d \n",costs[0][0], costs[0][2]);
                System.out.printf("dest 2|  %3d   %3d \n",costs[2][0], costs[2][2]);
                System.out.printf("     3|  %3d   %3d \n",costs[3][0], costs[3][2]);
                break;

            case 2:
                System.out.printf("                via     \n");
                System.out.printf("   D2 |    0     1    3 \n");
                System.out.printf("  ----|-----------------\n");
                System.out.printf("     0|  %3d   %3d   %3d\n",costs[0][0], costs[0][1],costs[0][3]);
                System.out.printf("dest 1|  %3d   %3d   %3d\n",costs[1][0], costs[1][1],costs[1][3]);
                System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][0], costs[3][1],costs[3][3]);
                break;
            case 3:
                System.out.printf("                via     \n");
                System.out.printf("   D3 |    0     2 \n");
                System.out.printf("  ----|-----------------\n");
                System.out.printf("     0|  %3d   %3d\n",costs[0][0],costs[0][2]);
                System.out.printf("dest 1|  %3d   %3d\n",costs[1][0],costs[1][2]);
                System.out.printf("     2|  %3d   %3d\n",costs[2][0],costs[2][2]);
                break;
        }
    }
}
