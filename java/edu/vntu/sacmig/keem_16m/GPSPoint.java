package edu.vntu.sacmig.keem_16m;

//Клас GPS-координати
public class GPSPoint{
    private Double latitude;
    private Double longitude;

    public GPSPoint(Double latitude, Double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Double getLatitude(){
        return this.latitude;
    }

    public Double getLongitude(){
        return this.longitude;
    }
}
