package edu.vntu.sacmig.keem_16m;

import java.util.ArrayList;
import java.util.List;

//Статичний клас з даними про поточний маршрут
// та завантаженим маршрутом (для відображення збереженого маршруту на карті)
public class GPSTracks {
    //Поточний маршрут
    public static class CurrentTrack {
        public static List<GPSPoint> points = new ArrayList<>();
        public static Double length;
        public static Double lostEnergy;
        public static Boolean isActive = false;
    }
    //Завантажений маршрут
    public static class LoadedTrack{
        public static List<GPSPoint> points = new ArrayList<>();
        public static String name;
        public static String user;
        public static Double length;
        public static String date;
    }
}
