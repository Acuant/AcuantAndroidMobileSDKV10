package com.acuant.sampleapp

import android.graphics.Bitmap
import com.acuant.acuantmobilesdk.models.IDResult

import com.acuant.acuantmobilesdk.models.ImageProcessingResult
import com.acuant.acuantmobilesdk.models.NFCData

/**
 * Created by tapasbehera on 11/11/16.
 */

object NFCStore {

    var image: Bitmap? = null
    var signature_image: Bitmap? = null
    var cardDetails: NFCData? = null
    var idResult: IDResult? = null
}
