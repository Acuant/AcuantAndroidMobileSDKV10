package com.acuant.sampleapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory.*

/**
 * Created by tapasbehera on 4/30/18.
 */
class CapturedImage {
    companion object {
        var bitmapImage: Bitmap? = null
        var acuantImage: com.acuant.acuantmobilesdk.models.Image? = null
        var barcodeString : String? = null
        fun setImage(image:android.media.Image){
            val buffer = image!!.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            bitmapImage = decodeByteArray(bytes, 0, bytes.size, null)
            image.close()
        }

        fun setImage(image:Bitmap?){
            bitmapImage = image
        }

        fun setImage(image:com.acuant.acuantmobilesdk.models.Image?){
            acuantImage = image
        }

        fun clear(){
            if(bitmapImage!=null){
                if(!bitmapImage!!.isRecycled) {
                    bitmapImage!!.recycle()
                }
            }
            if(acuantImage!=null && acuantImage!!.image!=null){
                if(!acuantImage!!.image.isRecycled) {
                    acuantImage!!.image.recycle()
                }
            }
        }
    }

}