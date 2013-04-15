/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.libs.adige;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import eu.hydrologis.JGrassModelsPlugin;

/**
 * The object for Pfafstetter numbers, supplying several methods to compare and analyse a
 * hierarchial network numbered following a Pfafstetter modified network.
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class PfafstetterNumber implements Comparator<PfafstetterNumber> {

    private String pfafstetterNumberString = null;
    private String pfafstetterUpToLastLeveL = null;
    private int order = -1;
    private List<Integer> ordersList = null;

    public PfafstetterNumber( String pfafstetterNumberString ) {
        this.pfafstetterNumberString = pfafstetterNumberString;

        ordersList = new ArrayList<Integer>();
        int lastDot = pfafstetterNumberString.lastIndexOf('.');
        if (lastDot == -1) {
            this.order = 1;
            ordersList.add(Integer.parseInt(pfafstetterNumberString));
            pfafstetterUpToLastLeveL = ""; //$NON-NLS-1$
        } else {
            String[] order = pfafstetterNumberString.split("\\."); //$NON-NLS-1$
            this.order = order.length;
            for( String string : order ) {
                ordersList.add(Integer.parseInt(string));
            }
            pfafstetterUpToLastLeveL = pfafstetterNumberString.substring(0, lastDot + 1);
        }

    }

    /**
     * @return the hierarchic order of the channel defined by this pfafstetter number
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the list of all the numbers composing this pfafstetter number
     */
    public List<Integer> getOrdersList() {
        return ordersList;
    }

    public String toString() {
        return pfafstetterNumberString;
    }

    /**
     * The pfaftetter string without the last level, usefull for comparison. The dot is added at the
     * end in order have defined levels. *
     * 
     * @return
     */
    public String toStringUpToLastLevel() {
        return pfafstetterUpToLastLeveL;
    }

    /**
     * Check if the number is of a certain order or minor to that order
     * 
     * @return
     */
    public boolean isOfOrderOrMinor( int order ) {
        if (this.order >= order) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the actual pfafstetter object is downstream or not of the passed argument
     * 
     * @param pfafstetterNumber
     * @return true if the actual obj is downstream of the passed one
     */
    public boolean isDownStreamOf( PfafstetterNumber pfafstetterNumber ) {
        /*
         * all the upstreams will have same numbers until the last dot
         */
        int lastDot = pfafstetterNumberString.lastIndexOf('.');
        String pre = pfafstetterNumberString.substring(0, lastDot + 1);
        String lastNum = pfafstetterNumberString.substring(lastDot + 1, pfafstetterNumberString
                .length());
        try {
            int lastNumInt = Integer.parseInt(lastNum);

            if (lastNumInt % 2 == 0) {
                // it has to be the last piece of a river, therefore no piece contained
                return false;
            } else {
                /*
                 * check if the passed one is upstream
                 */
                String pfaff = pfafstetterNumber.toString();
                if (pfaff.startsWith(pre)) {
                    // search for all those with a higher next number
                    String lastPart = pfaff.substring(lastDot + 1, pfaff.length());
                    if (Integer.parseInt(lastPart.split("\\.")[0]) >= lastNumInt) { //$NON-NLS-1$
                        return true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            JGrassModelsPlugin.log("PafafstetterNumber problem", e); //$NON-NLS-1$
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @return true if the queried piece is end piece of a reach, i.e. pfafstetter is even
     */
    public boolean isEndPiece() {
        return ordersList.get(ordersList.size() - 1) % 2 == 0 ? true : false;
    }

    public int compare( PfafstetterNumber p1, PfafstetterNumber p2 ) {
        List<Integer> p1OrdersList = p1.getOrdersList();
        List<Integer> p2OrdersList = p2.getOrdersList();

        int levels = p1OrdersList.size();
        if (p2OrdersList.size() < levels) {
            levels = p2OrdersList.size();
        }

        /*
         * check the numbers to the minor level of the two
         */
        for( int i = 0; i < levels; i++ ) {
            int thisone = p1OrdersList.get(i);
            int otherone = p2OrdersList.get(i);
            if (thisone > otherone) {
                /*
                 * if this has major number of the other, then this has to be sorted as minor of the
                 * other, following the pfafstetter logic that has major numbers towards valley
                 */
                return -1;
            } else if (thisone < otherone) {
                return 1;
            } else {
                // if they are equal, go on to the next level
                continue;
            }
        }

        return 0;
    }

    /**
     * Checks if two pfafstetter are connected upstream, i.e. p1 is more downstream than p2
     * 
     * @param p1
     * @param p2
     * @return
     */
    public synchronized static boolean areConnectedUpstream( PfafstetterNumber p1,
            PfafstetterNumber p2 ) {

        List<Integer> p1OrdersList = p1.getOrdersList();
        List<Integer> p2OrdersList = p2.getOrdersList();

        int levelDiff = p1OrdersList.size() - p2OrdersList.size();
        if (levelDiff == 0) {
            if (p1.toStringUpToLastLevel().equals(p2.toStringUpToLastLevel())) {
                int p1Last = p1OrdersList.get(p1OrdersList.size() - 1);
                int p2Last = p2OrdersList.get(p2OrdersList.size() - 1);
                if (p2Last == p1Last + 1 || p2Last == p1Last + 2) {
                    if (p1Last % 2 == 0) {
                        return false;
                    }
                    return true;
                }
            }
        } else if (levelDiff == -1) {
            if (p2.toString().startsWith(p1.toStringUpToLastLevel())) {
                int p2Last = p2OrdersList.get(p2OrdersList.size() - 1);
                if (p2Last != 1) {
                    return false;
                }
                int p1Last = p1OrdersList.get(p1OrdersList.size() - 1);
                int p2LastMinus1 = p2OrdersList.get(p2OrdersList.size() - 2);
                if (p2LastMinus1 == p1Last + 1 || p2Last == p1Last + 2) {
                    if (p1Last % 2 == 0) {
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * inverse of areConnectedUpstream
     * 
     * @param p1
     * @param p2
     * @return
     */
    public synchronized static boolean areConnectedDownstream( PfafstetterNumber p1,
            PfafstetterNumber p2 ) {
        return areConnectedUpstream(p2, p1);
    }

    public static void main( String[] args ) {
        PfafstetterNumber n1 = new PfafstetterNumber("2.5"); //$NON-NLS-1$
        PfafstetterNumber n2 = new PfafstetterNumber("2.6.4"); //$NON-NLS-1$
        PfafstetterNumber n3 = new PfafstetterNumber("2.4.3"); //$NON-NLS-1$
        PfafstetterNumber n4 = new PfafstetterNumber("2.7.1"); //$NON-NLS-1$
        PfafstetterNumber n5 = new PfafstetterNumber("2.4.16.45"); //$NON-NLS-1$
        PfafstetterNumber n6 = new PfafstetterNumber("2.6.2.1"); //$NON-NLS-1$
        PfafstetterNumber n7 = new PfafstetterNumber("2.7.6.5.2"); //$NON-NLS-1$
        PfafstetterNumber n8 = new PfafstetterNumber("2.7.6.2.1"); //$NON-NLS-1$
        PfafstetterNumber n9 = new PfafstetterNumber("2.6.2.7"); //$NON-NLS-1$
        List<PfafstetterNumber> list = new ArrayList<PfafstetterNumber>();
        list.add(n1);
        list.add(n2);
        list.add(n3);
        list.add(n4);
        list.add(n5);
        list.add(n6);
        list.add(n7);
        list.add(n8);

        System.out.println(n1.isDownStreamOf(n2));
        System.out.println(n1.isDownStreamOf(n4));
        System.out.println(n3.isDownStreamOf(n2));
        System.out.println(n3.isDownStreamOf(n5));
        System.out.println(n4.isDownStreamOf(n1));
        System.out.println(n6.isDownStreamOf(n7));
        System.out.println(n8.isDownStreamOf(n7));
        System.out.println();
        System.out.println(PfafstetterNumber.areConnectedUpstream(n1, n2));
        System.out.println(PfafstetterNumber.areConnectedUpstream(n1, n4));
        System.out.println(PfafstetterNumber.areConnectedUpstream(n3, n2));
        System.out.println(PfafstetterNumber.areConnectedUpstream(n3, n5));
        System.out.println(PfafstetterNumber.areConnectedUpstream(n4, n1));
        System.out.println(PfafstetterNumber.areConnectedUpstream(n4, n6));
        System.out.println(PfafstetterNumber.areConnectedUpstream(n8, n7));
        System.out.println(PfafstetterNumber.areConnectedUpstream(n6, n9));
        System.out.println();

        PfafstetterNumber[] array = list.toArray(new PfafstetterNumber[list.size()]);
        Arrays.sort(array, n1);
        for( PfafstetterNumber pfafstetterNumber : array ) {
            System.out.println(pfafstetterNumber.toString());
        }

    }

}
