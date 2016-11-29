package com.wiret.arbrowser;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by macintosh on 11/24/16.
 */
public class KmlParser {

    // We don't use namespaces
    private static final String ns = null;

    public ArrayList parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readPlaces(parser);
        } finally {
            in.close();
        }
    }


    public ArrayList readPlaces(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "kml");

        ArrayList entries = new ArrayList();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if(parser.getName().equals("Document")){

                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String name = parser.getName();
                    // Starts by looking for the entry tag
                    if (name.equals("Placemark")) {
                        entries.add(readPlace(parser));
                    } else {
                        skip(parser);
                    }
                }
            }
        }

        return entries;
    }

    private Place readPlace(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Placemark");
        Place place = new Place();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("name")) {
                place.name = readName(parser);
            } else if (name.equals("Point")) {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    if (parser.getName().equals("coordinates")) {
                        String coordinates = readText(parser);
                        place.lat = coordinates.split(",")[1].trim();
                        place.lon = coordinates.split(",")[0].trim();
                    }
                }
            } else if (name.equals("description")) {
                place.description = readText(parser);
            } else {
                skip(parser);
            }
        }

       return place;

    }

    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "name");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return title;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    public static class Place{
        public  String name;
        public  String description;
        public  String styleUrl;
        public  String lat;
        public  String lon;

    }

}
