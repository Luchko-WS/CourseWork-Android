package edu.vntu.sacmig.keem_16m;

//Статичний клас, що зберігає дані про активного користувача
public class CurrentUser {
    public static Integer selectedItemID = -1;
    public static Integer id = -1;
    public static String name = "невідомий";
    public static Float weight = -1.0f;
    public static Float lostEnergy = -1.0f;

    public static void setUserInformation(Integer par_id, String par_name, Float par_weight, Float par_lostEnergy){
        id = par_id;
        name = par_name;
        weight = par_weight;
        lostEnergy = par_lostEnergy;
    }
}
