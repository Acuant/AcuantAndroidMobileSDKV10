# AndroidMobileSDKV2

**Step - 0 : Setup:**

In the App manifest file, specify the permissions 
	
	<uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.NFC" />
    
Add the following meta information in the manifest file

	<meta-data
        android:name="com.google.android.gms.vision.DEPENDENCIES"
        android:value="barcode,face" />
    
SDK uses Android Vision library. Please make sure you put the following line in the dependencies

	implementation 'com.google.android.gms:play-services-vision:11.0.4+'
	
The following dependencies must be added for e-Chip verification

	implementation ('org.jmrtd:jmrtd:0.5.6')
    implementation ('org.ejbca.cvc:cert-cvc:1.4.3')
    implementation ('com.madgag.spongycastle:prov:1.54.0.0')
    implementation ('net.sf.scuba:scuba-sc-android:0.0.9')
    
Add the Acuant SDK dependency

	implementation project(':acuantsdk')
         
Keep the following ProGaurd rules while obfuscating

	-keep class * {
    native <methods>;
	}  
	
	-keep class org.ejbca.** { *; }
	-keepclassmembers class org.ejbca.** { *; }
	-keep class net.sf.scuba.** { *; }
	-keepclassmembers class net.sf.scuba.** { *; }
	-keep class org.jmrtd.** { *; }
	-keepclassmembers class org.jmrtd.** { *; } 
	
Please refer to the sample Sample App to check how to set up the permissions and dependencies correctly    
    
**Step - 1 : Initialize SDK :**

-	Set the endpoints

		val endPoints = Endpoints()
		endPoints!!.frmEndpoint = "https://frm.acuant.net/api/v1"
		endPoints!!.idEndpoint = "https://services.assureid.net"

-	Set the credentials

        val credential = Credential()
        credential!!.username = "username@acuantcorp.com"
        credential!!.password = "password"
        credential!!.subscription = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
        credential!!.endpoints = endPoints

-	Init Controller (e,g SDK)

        Controller.init(credential, object : InitializationListener {
            override fun initializationFinished(error: Error?) {
                if (error == null) {
                    // Sucess
                } else {
                    // Handle error here
                }
            }
        });
		
**Step - 2 : Capturing Image :**

Image capture is illustrated in the Sample App in the package "com.acuant.sampleapp.documentcapturecamera" . 

In the function *createCameraSource* of *DocumentCaptureActivity* , it is illustrated how to capture a document.

The illustrated capture code is only helpful if there is a requirement to detect PDF417 2D barcode in the image.

	// To create a document detector
	   documentDetector = controller.createDocumentDetector(context,this);
       DocumentCameraSource.Builder builder = new 		DocumentCameraSource.Builder(getApplicationContext(),documentDetector)
                .setFacing(DocumentCameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(60.0f);
                
                
    // The following method will start detecting barcode on the document with time in seconds.
    documentBarcodeDetector.startDetectingPdf417(2);
		 
**Step - 3 : Cropping Image :**

Once image is captured, its sent to the cropping library for cropping.

-	Setting card Attributes
	If the card type is known then set it to the correct type (e,g, CardType.ID1, CardType.ID2, or CardType.ID3) . If card type is ID2 then card width in inches needs to be set correctly. If the card type is either ID1 or ID3 then AUTO can be set to detect the card type by the cropping function.

		val cardAttributes = CardAtributes()
		if(!isHealthInsuranceCard) {
             //cardAttributes.cardType = CardType.ID2
             //cardAttributes.cardWidth = 4.13f
             cardAttributes.cardType = CardType.AUTO
       }
       
- Setting cropping options

	Set the CardAttributes, whether the captured images is an Health Insurance card or not and whether Image metrics (Sharpness and Glare) are required or not.
	
		val options = CroppingOptions()
		options.imageMetricsRequired = imageMetricsRequired
		options.cardAtributes = cardAttributes
		options.isHealthCard = isHealthInsuranceCard
		
-	Setting the Image to be cropped

		val data = CroppingData()
		data.image = image

- Crop

 		val acuantImage : Image = controller.crop(options,data);
 		
- Image class

		public class Image {
    			public Bitmap image;
    			public CardType detectedCardType;
    			public boolean hasImageMetrics;
    			public boolean isSharp;
    			public boolean hasGlare;
    			public float sharpnessGrade;
    			public float glareGrade;
    			public Error error;
		}

**Step - 4 : Facial Recognition :**

- Create a live face detector and camera

As demonstrated in the Sample App create a live face detector and a CameraSource as below :

		// The first parameter is the activity and the second one is the LiveFaceListener
		val liveFaceDetector = controller.createLiveFaceDetector(context, this)
		
		// Pass the detector to the CameraSource
       val mCameraSource = CameraSource.Builder(context, liveFaceDetector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedPreviewSize(320, 240)
                .setRequestedFps(60.0f)
                .setAutoFocusEnabled(true)
                .build()
   	 	
		
- LiveFaceListener Interface

	This interface has the following method
	
		override fun liveFaceDetailsCaptured(liveFaceDetails: LiveFaceDetails) {
        if (liveFaceDetails.error == null) {
            if(liveFaceDetails.isLiveFace){
                // A live face is detected the face image can be accessed as liveFaceDetails.image
            }
        	} else {
        	
        	}
    	}	
		
		
**Step 5 : Process captured images (Web Service call) :**

***Processing ID***


-	Set the Processing Options
        
        val cardAttributes = CardAtributes()
        // If it is ID2 type then set the cardWidth correctly
        //cardAttributes.cardType = CardType.ID2
        //cardAttributes.cardWidth = 4.13f
        cardAttributes.cardType = cardType
        
        val idProcessingOptions = IdOptions()
        idProcessingOptions.cardAttributes = cardAttributes
        
- Set the Processing Data
        
        val idProcessingData = IdData()
        imageProcessingData.frontImage = backImage // Bitmap
        imageProcessingData.backImage = frontImage // Bitmap
        imageProcessingData.barcodeString = capturedBarcodeString // String
        
- 	Process ID

        controller.processId(idProcessingData,idProcessingOptions,object : ImageProcessingListener{
            override fun imageProcessingFinished(result: ImageProcessingResult?) {
            		// Handle the response
            }

        })
        
            
***Processing Facial Match***

- Set Facial Data

		val facialMatchData = FacialMatchData()
		facialMatchData.faceImageOne = capturedFaceImage
		facialMatchData.faceImageTwo = capturedSelfieImage
                    
- Process Facial Match

 			controller.processFacialMatch(facialMatchData,object : FacialMatchListener{
 				override fun facialMatchFinished(result: FacialMatchResult?) {
                  // Handle response here
             	}
            })


**Step 6 : E-Passport chip reading :**

-	Initialize the Android NFC Adapter:

		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
-	Ensure that the permission is provided in runtime for API level 23 and above. 

-	The following SDK API can be used to listen to NFC tags available in an e-Passport
		
		controller.listenNFC(activity, nfcAdapter, listener)
		


- If an NFC Tag is successfully discovered, then the control will return to the method of the Activity that was previously overridden:

		@Override
    	protected void onNewIntent(Intent intent)
    	{
        	super.onNewIntent(intent);
        	
        	// Read the information from the tag as below
        	controller.readNFCTag(intent, docNumber, dateOfBirth, dateOfExpiry)
       }
        

-	Set the Listener to which the control will come after a chip is read successfully or an error occurs.


 		
 		override fun tagReadSucceeded(cardDetails: NFCData?, image: Bitmap?, sign_image: Bitmap?) {
 			// Handle Response
 		}

    	override fun tagReadFailed(message: String?) {
    		// Handle error
        
    	}