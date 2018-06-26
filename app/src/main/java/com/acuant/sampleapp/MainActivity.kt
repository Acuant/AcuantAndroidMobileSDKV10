package com.acuant.sampleapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.acuant.acuantmobilesdk.*
import com.acuant.acuantmobilesdk.models.*
import com.acuant.sampleapp.backgroundtasks.CroppingTask
import com.acuant.sampleapp.backgroundtasks.CroppingTaskListener
import com.acuant.sampleapp.utils.CommonUtils
import com.acuant.sampleapp.utils.DialogUtils
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64
import com.acuant.sampleapp.facialcamera.FacialLivelinessActivity
import com.acuant.sampleapp.documentcapturecamera.DocumentCaptureActivity
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private var progressDialog: ProgressDialog? = null
    private var capturedFrontImage: Image? = null
    private var capturedBackImage: Image? = null
    private var capturedSelfieImage: Bitmap? = null
    private var capturedFaceImage: Bitmap? = null
    private var capturedBarcodeString: String? = null
    private var frontCaptured: Boolean = false
    private var isHealthCard: Boolean = false
    private var credential : Credential? = null
    private var insruanceButton : Button? = null
    private var capturingImageData : Boolean = true
    private var capturingSelfieImage : Boolean = false
    private var capturingFacialMatch : Boolean = false
    private var facialResultString : String? = null

    fun cleanUpTransaction(){
        capturedFrontImage = null
        capturedBackImage = null
        capturedSelfieImage = null
        capturedFaceImage = null
        capturedBarcodeString = null

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        insruanceButton = findViewById(R.id.main_health_card)
        insruanceButton!!.visibility = View.GONE
        progressDialog = DialogUtils.showProgessDialog(this, "Initializing...")

        val endPoints = Endpoints()
        endPoints!!.frmEndpoint = "https://frm.acuant.net/api/v1"
        endPoints!!.healthInsuranceEndpoint = "https://cssnwebservices.com"
        endPoints!!.idEndpoint = "https://services.assureid.net"

        credential = Credential()
        credential!!.username = "xxxxxxx@acuantcorp.com"
        credential!!.password = "xxxxxxxxxxxxxxx"
        credential!!.subscription = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
        credential!!.endpoints = endPoints

        Controller.init(credential, object : InitializationListener {
            override fun initializationFinished(error: Error?) {
                if (error == null) {
                    this@MainActivity.runOnUiThread {
                        DialogUtils.dismissDialog(progressDialog)
                    }
                    AppInstance.instance.setController(Controller.getInstance())
                    if(AppInstance.instance.getController()!!.isHealthInsuranceCardAllowed()){
                        insruanceButton!!.visibility = View.VISIBLE
                    }
                } else {
                    this@MainActivity.runOnUiThread {
                        DialogUtils.dismissDialog(progressDialog)
                    }
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Error")
                    alert.setMessage(error.errorDescription)
                    alert.setPositiveButton("OK") { dialog, whichButton ->
                        dialog.dismiss()
                    }
                    alert.show()
                }
            }
        });
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Constants.REQUEST_CAMERA_PHOTO) {
            val image = CapturedImage.bitmapImage
            capturedBarcodeString = null
            if(CapturedImage.barcodeString != null) {
                capturedBarcodeString = CapturedImage.barcodeString
            }else{
                capturedBarcodeString = null
            }
            if (AppInstance.instance.getController() != null) {
                progressDialog = DialogUtils.showProgessDialog(this, "Cropping...")
                var croppingTask = CroppingTask(AppInstance.instance.getController()!!, image!!, isHealthCard, true, !frontCaptured, object : CroppingTaskListener {
                    override fun croppingFinished(image: Image?, isFrontImage: Boolean) {
                        this@MainActivity.runOnUiThread {
                            DialogUtils.dismissDialog(progressDialog)
                        }
                        //image!!.image = BitmapFactory.decodeResource(resources,R.drawable.ci_bavro_gheorghe_052016_resized)
                        CapturedImage.acuantImage = correctBitmapOrientation(image)
                        showConfirmation(isFrontImage,false)
                    }

                });
                croppingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null)
            } else {
                this@MainActivity.runOnUiThread {
                    DialogUtils.dismissDialog(progressDialog)
                }
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("Error")
                alert.setMessage("SDK is not initialized")
                alert.setPositiveButton("OK") { dialog, whichButton ->
                    dialog.dismiss()
                }
                alert.show()
            }
        } else if (resultCode == Constants.REQUEST_CONFIRMATION) {
            val isFront = data!!.getBooleanExtra("IsFrontImage", true)
            val isConfirmed = data!!.getBooleanExtra("Confirmed", true)
            val isBarcode = data!!.getBooleanExtra("IsBarcode", false)
            if (isConfirmed) {
                if (isFront) {
                    frontCaptured = true
                    capturedFrontImage = CapturedImage.acuantImage
                    if (isHealthCard) {
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Message")
                        alert.setMessage("Scan back side of health insurance card.")
                        alert.setPositiveButton("OK") { dialog, whichButton ->
                            dialog.dismiss()
                            showDocumentCaptureCamera()
                        }
                        alert.setNegativeButton("CANCEL") { dialog, whichButton ->
                            dialog.dismiss()
                        }
                        alert.show()
                    }else if (CapturedImage.acuantImage!!.detectedCardType == CardType.ID1) {
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Message")
                        alert.setMessage("Scan back side of driving license card.")
                        alert.setPositiveButton("OK") { dialog, whichButton ->
                            dialog.dismiss()
                            showDocumentCaptureCamera()
                        }
                        alert.setNegativeButton("CANCEL") { dialog, whichButton ->
                            dialog.dismiss()
                        }
                        alert.show()
                    } else if (CapturedImage.acuantImage!!.detectedCardType == CardType.ID3) {
                        if(AppInstance.instance.getController()!!.isFacialAllowed()) {
                            val alert = AlertDialog.Builder(this@MainActivity)
                            alert.setTitle("Message")
                            alert.setMessage("Capture Selfie Image")
                            alert.setPositiveButton("OK") { dialog, whichButton ->
                                dialog.dismiss()
                                processImages(CardType.ID3)
                                showFrontCamera()
                            }
                            alert.setNegativeButton("CANCEL") { dialog, whichButton ->
                                dialog.dismiss()
                            }
                            alert.show()
                        }
                    }else if(CapturedImage.acuantImage!!.detectedCardType == CardType.ID2){
                        if(AppInstance.instance.getController()!!.isFacialAllowed()) {
                            val alert = AlertDialog.Builder(this@MainActivity)
                            alert.setTitle("Message")
                            alert.setMessage("Capture Selfie Image")
                            alert.setPositiveButton("OK") { dialog, whichButton ->
                                dialog.dismiss()
                                processImages(CardType.ID2)
                                showFrontCamera()
                            }
                            alert.setNegativeButton("CANCEL") { dialog, whichButton ->
                                dialog.dismiss()
                            }
                            alert.show()
                        }
                    }
                } else {
                    capturedBackImage = CapturedImage.acuantImage
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Message")
                    if(!isHealthCard) {
                        if(AppInstance.instance.getController()!!.isFacialAllowed()) {
                            if(capturedBarcodeString!=null && capturedBarcodeString!!.trim()!!.length>0){
                                alert.setMessage("Following barcode is captured.\n\n"
                                        + "Barcode String :\n\n"
                                        + capturedBarcodeString!!.subSequence(0, (capturedBarcodeString!!.length * 0.25).toInt())
                                        + "...\n\n"
                                        + "Capture Selfie Image now.")
                            }else {
                                alert.setMessage("Capture Selfie Image")
                            }
                            alert.setPositiveButton("OK") { dialog, whichButton ->
                                dialog.dismiss()
                                processImages(CardType.ID1)
                                showFrontCamera()
                            }
                        }
                    }else{
                        alert.setMessage("Process captured Card?")
                        alert.setPositiveButton("OK") { dialog, whichButton ->
                            dialog.dismiss()
                            processHealthCard()
                        }
                    }
                    alert.setNegativeButton("CANCEL") { dialog, whichButton ->
                        dialog.dismiss()
                    }
                    alert.show()
                }
            } else {
                showDocumentCaptureCamera()
            }
        } else if(resultCode == Constants.REQUEST_CAMERA_SELFIE){
            capturingSelfieImage = false
            capturedSelfieImage = CapturedImage.bitmapImage
            processFacialMatch()

        }
    }

    // ID/Passport Clicked
    fun idPassPortClicked(view: View) {
        frontCaptured = false
        isHealthCard = false
        cleanUpTransaction()
        showDocumentCaptureCamera()
    }

    // Health Insurance Clicked
    fun healthInsuranceClicked(view: View) {
        frontCaptured = false
        isHealthCard = true
        cleanUpTransaction()
        showDocumentCaptureCamera()
    }

    //Show Rear Camera to Capture Image of ID,Passport or Health Insruance Card
    fun showDocumentCaptureCamera() {
        CapturedImage.barcodeString = null
        val cameraIntent = Intent(
                this@MainActivity,
                DocumentCaptureActivity::class.java
        )
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_PHOTO)
    }

    //Show Front Camera to Capture Live Selfie
    fun showFrontCamera() {
        capturingSelfieImage = true;
        val cameraIntent = Intent(
                this@MainActivity,
                FacialLivelinessActivity::class.java
        )
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_SELFIE)
    }

    //process images
    fun processHealthCard(){

    }
    fun processImages(cardType: CardType) {
        MainActivity@capturingImageData = true
        val imageProcessingOptions = IdOptions()
        val cardAttributes = CardAtributes()

        //cardAttributes.cardType = CardType.ID2
        //cardAttributes.cardWidth = 4.13f

        cardAttributes.cardType = cardType
        imageProcessingOptions.cardAttributes = cardAttributes
        val imageProcessingData = IdData()
        if(capturedFrontImage != null) {
            imageProcessingData.frontImage = (capturedFrontImage!!.image)
        }
        if(capturedBackImage != null) {
            imageProcessingData.backImage = (capturedBackImage!!.image)
        }

        if(capturedBarcodeString != null) {
            imageProcessingData.barcodeString = capturedBarcodeString
            capturedBarcodeString = null
        }
        progressDialog = DialogUtils.showProgessDialog(this, "Processing ...")
        AppInstance.instance.getController()!!.processId(imageProcessingData,imageProcessingOptions,object : ImageProcessingListener{
            override fun imageProcessingFinished(result: ImageProcessingResult?) {
                if(result is HealthInsuranceCardResult) {
                    this@MainActivity.runOnUiThread {
                        DialogUtils.dismissDialog(progressDialog)
                    }
                    val resultStr = CommonUtils.stringFromResult(result)
                    showHealthCardResults(null,null,null,result.reformattedImage,result.reformattedImageTwo,null,null,resultStr)
                }else if(result is ImageProcessingResult) {
                    if(result.error!=null || result==null){
                        this@MainActivity.runOnUiThread {
                            DialogUtils.dismissDialog(progressDialog)
                        }
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Error")
                        alert.setMessage(result.error.errorDescription)
                        alert.setPositiveButton("OK") {dialog,whichButton->
                            dialog.dismiss()
                        }
                        alert.show()
                        return;
                    }else if((result as IDResult).fields==null || (result as IDResult).fields.dataFieldReferences==null){
                        this@MainActivity.runOnUiThread {
                            DialogUtils.dismissDialog(progressDialog)
                        }
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Error")
                        alert.setMessage("Unknown error happened.Could not extract data")
                        alert.setPositiveButton("OK") {dialog,whichButton->
                            dialog.dismiss()
                        }
                        alert.show()
                        return;

                    }
                    var instanceID = result.instanceID
                    var docNumber = ""
                    var cardType: CardType = CardType.ID1
                    var frontImageUri: String? = null
                    var backImageUri: String? = null
                    var signImageUri: String? = null
                    var faceImageUri: String? = null
                    var resultString: String? = ""
                    var fieldReferences = result.fields.dataFieldReferences
                    for (reference in fieldReferences) {
                        if (reference.key.equals("Document Class Name") && reference.type.equals("string")) {
                            if (reference.value.equals("Driver License")) {
                                cardType = CardType.ID1
                            } else if (reference.value.equals("Passport")) {
                                cardType = CardType.ID3
                            }
                        } else if (reference.key.equals("Document Number") && reference.type.equals("string")) {
                            docNumber = reference.value;
                        } else if (reference.key.equals("Photo") && reference.type.equals("uri")) {
                            faceImageUri = reference.value;
                        } else if (reference.key.equals("Signature") && reference.type.equals("uri")) {
                            signImageUri = reference.value;
                        }
                    }

                    for (image in result.images.images) {
                        if (image.side == 0) {
                            frontImageUri = image.uri
                        } else if (image.side == 1) {
                            backImageUri = image.uri
                        }
                    }

                    for (reference in fieldReferences) {
                        if (reference.type.equals("string")) {
                            resultString = resultString + reference.key + ":" + reference.value + System.lineSeparator()
                        }
                    }

                    resultString = "Authentication Result : " +
                            AuthenticationResult.getString(Integer.parseInt(result.result)) +
                            System.lineSeparator() +
                            System.lineSeparator() +
                            resultString;

                    thread{
                        val frontImage = loadAssureIDImage(frontImageUri, credential)
                        val backImage = loadAssureIDImage(backImageUri, credential)
                        val faceImage = loadAssureIDImage(faceImageUri, credential)
                        val signImage = loadAssureIDImage(signImageUri, credential)
                        capturedFaceImage = faceImage
                        MainActivity@capturingImageData = false
                        if(AppInstance.instance.getController()!!.isFacialAllowed){
                            while (capturingSelfieImage){
                                Thread.sleep(100)
                            }
                        }
                        this@MainActivity.runOnUiThread {
                            DialogUtils.dismissDialog(progressDialog)
                            showResults(result.biographic.birthDate,result.biographic.expirationDate,docNumber,frontImage,backImage,faceImage,signImage,resultString,cardType)
                            AppInstance.instance.getController()!!.deleteInstance(instanceID,object : DeleteListener{
                                override fun instanceDeleted(success: Boolean) {
                                     if(!success){
                                         // Handle error
                                     }else{
                                         Log.d("DELETE","Instance Deleted successfully")
                                     }
                                }

                            })
                        }
                    }

                }
            }

        })

    }

    //process Facial Match
    fun processFacialMatch(){
        MainActivity@capturingFacialMatch = true
        thread{
            while(MainActivity@capturingImageData){
                Thread.sleep(100)
            }
            this@MainActivity.runOnUiThread {
                val facialMatchData = FacialMatchData()
                facialMatchData.faceImageOne = capturedFaceImage
                facialMatchData.faceImageTwo = capturedSelfieImage
                if(progressDialog!=null && progressDialog!!.isShowing) {
                    DialogUtils.dismissDialog(progressDialog)
                }
                progressDialog = DialogUtils.showProgessDialog(this, "Matching selfie ...")
                AppInstance.instance.getController()!!.processFacialMatch(facialMatchData,object : FacialMatchListener{
                    override fun facialMatchFinished(result: FacialMatchResult?) {
                        capturingFacialMatch = false
                        this@MainActivity.runOnUiThread {
                            DialogUtils.dismissDialog(progressDialog)
                            if (result!!.error == null) {
                                val resultStr = CommonUtils.stringFromFacialMatchResult(result)
                                facialResultString = resultStr
                            } else {
                                val alert = AlertDialog.Builder(this@MainActivity)
                                alert.setTitle("Error")
                                alert.setMessage(result!!.error.errorDescription)
                                alert.setPositiveButton("OK") { dialog, whichButton ->
                                    dialog.dismiss()
                                }
                                alert.show()
                            }
                        }
                    }

                })

            }
        }
    }

    //Show Confirmation UI
    fun showConfirmation(isFrontImage: Boolean,isBarcode:Boolean) {
        val confirmationIntent = Intent(
                this@MainActivity,
                ConfirmationActivity::class.java
        )
        confirmationIntent.putExtra("IsFrontImage", isFrontImage)
        confirmationIntent.putExtra("IsBarcode", isBarcode)
        startActivityForResult(confirmationIntent, Constants.REQUEST_CONFIRMATION)
    }

    //Show Health card Results
    fun showHealthCardResults(dateOfBirth:String? , dateOfExpiry:String? ,documentNumber:String? ,frontImage : Bitmap?,backImage : Bitmap?,faceImage : Bitmap?,signImage:Bitmap?,resultString : String?){
        ProcessedData.cleanup()
        ProcessedData.frontImage = frontImage
        ProcessedData.backImage = backImage
        ProcessedData.faceImage = faceImage
        ProcessedData.signImage = signImage
        ProcessedData.dateOfBirth= dateOfBirth
        ProcessedData.dateOfExpiry= dateOfExpiry
        ProcessedData.documentNumber= documentNumber
        ProcessedData.isHealthCard = true
        ProcessedData.formattedString = resultString
        val resultIntent = Intent(
                this@MainActivity,
                ResultActivity::class.java
        )
        startActivity(resultIntent)
    }

    // Show ID Result
    fun showResults(dateOfBirth:String? , dateOfExpiry:String? ,documentNumber:String? ,frontImage : Bitmap?,backImage : Bitmap?,faceImage : Bitmap?,signImage:Bitmap?,resultString : String?,cardType:CardType){
        ProcessedData.cleanup()
        ProcessedData.frontImage = frontImage
        ProcessedData.backImage = backImage
        ProcessedData.faceImage = faceImage
        ProcessedData.signImage = signImage
        ProcessedData.dateOfBirth= dateOfBirth
        ProcessedData.dateOfExpiry= dateOfExpiry
        ProcessedData.documentNumber= documentNumber
        ProcessedData.cardType = cardType
        if(!isHealthCard && AppInstance.instance.getController()!!.isFacialAllowed){
            thread {
                while(capturingFacialMatch){
                    Thread.sleep(100)
                }
                this@MainActivity.runOnUiThread {
                    ProcessedData.formattedString = facialResultString + System.lineSeparator() + resultString
                    val resultIntent = Intent(
                            this@MainActivity,
                            ResultActivity::class.java
                    )
                    if(progressDialog!=null && progressDialog!!.isShowing) {
                        DialogUtils.dismissDialog(progressDialog)
                    }
                    startActivity(resultIntent)
                }

            }
            return
        }
        ProcessedData.formattedString = resultString
        val resultIntent = Intent(
                this@MainActivity,
                ResultActivity::class.java
        )
        if(progressDialog!=null && progressDialog!!.isShowing) {
            DialogUtils.dismissDialog(progressDialog)
        }
        startActivity(resultIntent)
    }

    //Correct orientation
    fun correctBitmapOrientation(image: Image?): Image? {
        if (image != null && image.image != null && image.image.getHeight() > image.image.getWidth()) {
            val mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val display = mWindowManager.defaultDisplay
            var angle = 0
            when (display.rotation) {
                Surface.ROTATION_0 // This is display orientation
                -> angle = 270 // This is camera orientation
                Surface.ROTATION_90 -> angle = 180
                Surface.ROTATION_180 -> angle = 90
                Surface.ROTATION_270 -> angle = 0
                else -> angle = 180
            }

            val matrix = Matrix()
            matrix.postRotate(angle.toFloat())
            image.image = Bitmap.createBitmap(image.image, 0, 0, image.image.getWidth(), image.image.getHeight(), matrix, true)
            return image
        }
        return image
    }

    fun dpToPx(dp: Int): Int {
        val displayMetrics = applicationContext.resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    fun loadAssureIDImage(url : String? , credential: Credential?):Bitmap? {
        if(url!=null && credential!=null) {
            val c = URL(url).openConnection() as HttpURLConnection
            val userpass = credential.username + ":" + credential.password
            val basicAuth = "Basic " + String(Base64.encode(userpass.toByteArray(),Base64.DEFAULT))
            c.setRequestProperty("Authorization", basicAuth)
            c.useCaches = false
            c.connect()
            val bmImg = BitmapFactory.decodeStream(c.inputStream)
            return bmImg
        }
        return null
    }

}
