package com.acuant.sampleapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import com.acuant.acuantmobilesdk.models.CardType
import android.view.Gravity
import android.R.attr.gravity
import android.graphics.*
import android.view.ViewGroup
import android.widget.*
import android.graphics.Paint.Align
import android.graphics.Paint.ANTI_ALIAS_FLAG




class ConfirmationActivity : AppCompatActivity() {

    var IsFrontImage: Boolean = true
    var IsBarcode: Boolean = false
    var isHealthCard: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)
        IsFrontImage = intent.getBooleanExtra("IsFrontImage", true)
        isHealthCard = intent.getBooleanExtra("IsHealthCard", false)
        IsBarcode = intent.getBooleanExtra("IsBarcode", false)

        if(CapturedImage.barcodeString != null){
            val barcodeText = findViewById<TextView>(R.id.barcodeText)
            barcodeText.setText("Barcode :" + CapturedImage.barcodeString!!.substring(0,(CapturedImage.barcodeString!!.length*0.2).toInt())+"...");
        }

        if (CapturedImage.acuantImage != null && CapturedImage.acuantImage!!.error == null) {

            val generalMessageText = findViewById<TextView>(R.id.generalMessageText)
            if (generalMessageText != null && CapturedImage.acuantImage!!.image != null) {
                generalMessageText.setText("Ensure all texts are visible.")
            } else if (CapturedImage.acuantImage!!.image == null) {
                generalMessageText.setText("Could not crop image.")
            }

            val cardTypeText = findViewById<TextView>(R.id.cardTypeText)
            if (cardTypeText != null && CapturedImage.acuantImage!!.image != null) {
                if (isHealthCard) {
                    cardTypeText.setText("Card Type : Health Insurance Card");
                } else if (CapturedImage.acuantImage!!.detectedCardType == CardType.ID3) {
                    cardTypeText.setText("Card Type : ID3");
                } else if (CapturedImage.acuantImage!!.detectedCardType == CardType.ID1) {
                    cardTypeText.setText("Card Type : ID1");
                }  else if (CapturedImage.acuantImage!!.detectedCardType == CardType.ID2) {
                    cardTypeText.setText("Card Type : ID2");
                }
            }

            if (CapturedImage.acuantImage != null && CapturedImage.acuantImage!!.hasImageMetrics) {
                val sharpnessText = findViewById<TextView>(R.id.sharpnessText)
                if (sharpnessText != null && CapturedImage.acuantImage!!.image != null) {
                    if (CapturedImage.acuantImage!!.isSharp) {
                        sharpnessText.setText("It is a sharp image. Sharpness Garde : " + CapturedImage.acuantImage!!.sharpnessGrade)
                    } else {
                        sharpnessText.setText("It is a blurry image. Sharpness Garde : " + CapturedImage.acuantImage!!.sharpnessGrade)
                    }
                }

                val glareText = findViewById<TextView>(R.id.glareText)
                if (glareText != null && CapturedImage.acuantImage!!.image != null) {
                    if (CapturedImage.acuantImage!!.hasGlare) {
                        glareText.setText("Image has glare. Glare Garde : " + CapturedImage.acuantImage!!.glareGrade)
                    } else {
                        glareText.setText("Image doesn't have glare. Glare Garde : " + CapturedImage.acuantImage!!.glareGrade)
                    }
                }
            }

            val confrimationImage = findViewById<ImageView>(R.id.confrimationImage)
            if (confrimationImage != null && CapturedImage.acuantImage != null && CapturedImage.acuantImage!!.image != null) {
                confrimationImage.setImageBitmap(CapturedImage.acuantImage!!.image)
                confrimationImage.scaleType = ImageView.ScaleType.FIT_CENTER

                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels

                val lp = confrimationImage.layoutParams
                lp.height = (height * 0.4).toInt()
                confrimationImage.layoutParams = lp

            }
        } else {
            val confirmButton = findViewById<Button>(R.id.confirmButton)
            confirmButton.visibility = View.GONE
            val generalMessageText = findViewById<TextView>(R.id.generalMessageText)
            generalMessageText.setText("Could not crop image.")

            val confrimationImage = findViewById<ImageView>(R.id.confrimationImage)
            if (confrimationImage != null) {
                confrimationImage.scaleType = ImageView.ScaleType.FIT_CENTER
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels

                val lp = confrimationImage.layoutParams
                lp.height = (height * 0.4).toInt()
                confrimationImage.layoutParams = lp
                confrimationImage.setImageBitmap(textAsBitmap("Could not crop",200.0f,Color.RED))
                confrimationImage.visibility = View.VISIBLE
            }

        }

    }


    fun confirmClicked(view: View) {
        val result = Intent()
        result.putExtra("Confirmed", true)
        result.putExtra("IsFrontImage", IsFrontImage)
        this@ConfirmationActivity.setResult(Constants.REQUEST_CONFIRMATION, result)
        this@ConfirmationActivity.finish()

    }

    fun retryClicked(view: View) {
        val result = Intent()
        result.putExtra("Confirmed", false)
        result.putExtra("IsFrontImage", IsFrontImage)
        result.putExtra("IsBarcode", IsBarcode)
        this@ConfirmationActivity.setResult(Constants.REQUEST_CONFIRMATION, result)
        this@ConfirmationActivity.finish()
    }

    fun textAsBitmap(text: String, textSize: Float, textColor: Int): Bitmap {
        val paint = Paint(ANTI_ALIAS_FLAG)
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        val baseline = -paint.ascent() // ascent() is negative
        val width = (paint.measureText(text) + 0.5f).toInt() // round
        val height = (baseline + paint.descent() + 0.5f).toInt()
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(text, 0.0f, baseline, paint)
        return image
    }
}
