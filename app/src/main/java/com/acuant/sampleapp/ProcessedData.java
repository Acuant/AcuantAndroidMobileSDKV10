package com.acuant.sampleapp;

import android.graphics.Bitmap;

import com.acuant.acuantmobilesdk.models.CardType;

/**
 * Created by tapasbehera on 5/16/18.
 */

public class ProcessedData {
    public static Bitmap frontImage = null;
    public static Bitmap backImage = null;
    public static Bitmap faceImage = null;
    public static Bitmap signImage = null;
    public static CardType cardType = CardType.ID1;
    public static String formattedString="";
    public static String dateOfBirth = null;
    public static String dateOfExpiry = null;
    public static String documentNumber = null;
    public static boolean isHealthCard = false;
    public static void cleanup(){
        frontImage = null;
        backImage = null;
        faceImage = null;
        signImage = null;
        cardType = CardType.ID1;
        formattedString="";
        dateOfBirth = null;
        dateOfExpiry = null;
        documentNumber = null;
        isHealthCard = false;
    }
}
