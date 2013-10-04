package cz.cvut.kbss.pubby;

/**
 * Petr Křemen, 2013
 * petr@sio2.cz
 */
public class Point {
    double lng, lat;

    public Point(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public double getLat() {
        return lat;
    }
}