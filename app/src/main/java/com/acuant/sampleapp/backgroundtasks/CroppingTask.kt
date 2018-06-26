package com.acuant.sampleapp.backgroundtasks

import android.graphics.Bitmap
import android.os.AsyncTask
import com.acuant.acuantmobilesdk.Controller
import com.acuant.acuantmobilesdk.models.*


/**
 * Created by tapasbehera on 4/30/18.
 */
class CroppingTask constructor(val c: Controller, val originalImage:Bitmap, val isHealthCard : Boolean, val imageMetrics:Boolean, val isFrontImage:Boolean, val listener: CroppingTaskListener) : AsyncTask<String, String, String>() {

    var controller : Controller = c
    var image : Bitmap? = originalImage
    var isHealthInsuranceCard : Boolean = isHealthCard
    var taskListener : CroppingTaskListener? = listener
    var imageMetricsRequired : Boolean = imageMetrics
    var frontImage : Boolean = isFrontImage
    private var acuantImage : Image? = null

    override fun onPreExecute() {
        super.onPreExecute()

    }

    override fun doInBackground(vararg p0: String?): String {
        if(image!=null) {
            val cardAttributes = CardAtributes()
            if(!isHealthInsuranceCard) {
                //cardAttributes.cardType = CardType.ID2
                //cardAttributes.cardWidth = 4.13f
                cardAttributes.cardType = CardType.AUTO
            }

            val options = CroppingOptions()
            options.imageMetricsRequired = imageMetricsRequired
            options.cardAtributes = cardAttributes
            options.isHealthCard = isHealthInsuranceCard

            val data = CroppingData()
            data.image = image

            acuantImage = controller.crop(options,data)

        }
        return ""
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        if(taskListener!=null){
            taskListener!!.croppingFinished(acuantImage,frontImage)
        }
    }
}