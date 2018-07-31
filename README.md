# Acuant Android Mobile SDK v10


**Last updated – June 27, 2018**

Copyright <sup>©</sup> 2003-2018 Acuant Inc. All rights reserved.

This document contains proprietary and confidential 
information and creative works owned by Acuant and its respective
licensors, if any. Any use, copying, publication, distribution, display,
modification, or transmission of such technology in whole or in part in
any form or by any means without the prior express written permission of
Acuant is strictly prohibited. Except where expressly provided by Acuant
in writing, possession of this information shall not be
construed to confer any license or rights under any Acuant intellectual
property rights, whether by estoppel, implication, or otherwise.

AssureID and *i-D*entify are trademarks of Acuant Inc. Other Acuant product or service names or logos referenced this document are either trademarks or registered trademarks of Acuant.

All 3M trademarks are trademarks of Gemalto Inc.

Windows<sup>®</sup> is a registered trademark of Microsoft Corporation.

Certain product, service, or company designations for companies other
than Acuant may be mentioned in this document for identification
purposes only. Such designations are often claimed as trademarks or
service marks. In all instances where Acuant is aware of a claim, the
designation appears in initial capital or all capital letters. However,
you should contact the appropriate companies for more complete
information regarding such designations and their registration status.

**June 2018**

<p>Acuant Inc.</p>
<p>6080 Center Drive, Suite 850</p>
<p>Los Angeles, CA 90045</p>
<p>==================</p>


# Introduction

<p>Acuant Web Services supports data extraction from driver’s licenses, state IDs, other government issued IDs, custom IDs, driver’s licenses, barcodes, and passports. It also supports document authentication and facial recognition to verify and authenticate the identity.</p>

<p>This document contains a detailed description of all functions that developers need to integrate with the Acuant Android Mobile SDK.</p>




## Setup ##


**Prerequisites:** API version 21 or higher.

1. **Open the App manifest file.**

1. **Specify the permissions in the App manifest file:** 
	
		<uses-permission android:name="android.permission.INTERNET" />
		<uses-permission android:name="android.permission.CAMERA" />
		
		// Required if reading e-Chip in the e-Passports
		<uses-permission android:name="android.permission.NFC" />

1. **Add the following meta information in the manifest file:**

		<meta-data
        android:name="com.google.android.gms.vision.DEPENDENCIES"
        android:value="barcode,face" />
    
1. **Include the following line in the dependencies (The Android Mobile SDK uses the Android Vision library):** 

		implementation 'com.google.android.gms:play-services-vision:11.0.4+'
	
1. **Add the following dependencies for e-Chip verification:**

		implementation ('org.jmrtd:jmrtd:0.5.6')
		implementation ('org.ejbca.cvc:cert-cvc:1.4.3')
		implementation ('com.madgag.spongycastle:prov:1.54.0.0')
		implementation ('net.sf.scuba:scuba-sc-android:0.0.9')
		
		//Note : Required for e-Chip reading in e-Passports

1. **Add the Acuant SDK dependency:**

		implementation project(':acuantsdk')

1. **Keep the following ProGuard rules while obfuscating:**

		-keep class * {
			native <methods>;
		}  
	
		-keep class org.ejbca.** { *; }
		-keepclassmembers class org.ejbca.** { *; }
		-keep class net.sf.scuba.** { *; }
		-keepclassmembers class net.sf.scuba.** { *; }
		-keep class org.jmrtd.** { *; }
		-keepclassmembers class org.jmrtd.** { *; } 
	
**Note:**  See the Sample App to see how to set up the permissions and dependencies correctly.    
    
## Initializing the SDK ##

1.	**Set the endpoints:**

		val endPoints = Endpoints()
		endPoints!!.frmEndpoint = "https://frm.acuant.net/api/v1"
		endPoints!!.idEndpoint = "https://services.assureid.net"

1. **Set the credentials:**

        val credential = Credential()
        credential!!.username = "username@acuantcorp.com"
        credential!!.password = "password"
        credential!!.subscription = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
        credential!!.endpoints = endPoints

1. **Initialize the Controller:**

        Controller.init(credential, object : InitializationListener {
            override fun initializationFinished(error: Error?) {
                if (error == null) {
                    // Sucess
                } else {
                    // Handle error here
                }
            }
        });
		
## Image capture ##

Image capture is illustrated in the Sample App in the "**com.acuant.sampleapp.documentcapturecamera**" package. See the *createCameraSource* of *DocumentCaptureActivity* function that illustrates how to capture a document.

**Note:**  The illustrated capture code is only helpful if there is a requirement to detect PDF417 2D barcode in the image.

	// To create a document detector
	   documentDetector = controller.createDocumentDetector(context,this);
       DocumentCameraSource.Builder builder = new 		DocumentCameraSource.Builder(getApplicationContext(),documentDetector)
                .setFacing(DocumentCameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(60.0f);
                
                
    // The following method will start detecting barcode on the document with time in seconds.
    documentDetector.startDetectingPdf417(2);
		 
## Image cropping ##

After an image is captured, it is sent to the cropping library for cropping.

1. **Set card attributes:**

	If the card type is known then set it to the correct type (e,g, CardType.ID1, CardType.ID2, or CardType.ID3) . If card type is ID2 then card width in inches needs to be set correctly. If the card type is either ID1 or ID3 then AUTO can be set to detect the card type by the cropping function.

		val cardAttributes = CardAtributes()
		if(!isHealthInsuranceCard) {
             //cardAttributes.cardType = CardType.ID2
             //cardAttributes.cardWidth = 4.13f
             cardAttributes.cardType = CardType.AUTO
		}
		
	*Note:*	[https://en.wikipedia.org/wiki/ISO/IEC_7810](https://en.wikipedia.org/wiki/ISO/IEC_7810)
       
1. **Set the cropping options:**

	Set the CardAttributes, whether the captured images is an Health Insurance card or not and whether Image metrics (Sharpness and Glare) are required or not.
	
		val options = CroppingOptions()
		options.imageMetricsRequired = imageMetricsRequired
		options.cardAtributes = cardAttributes
		options.isHealthCard = isHealthInsuranceCard
		

1. **Set the Image to be cropped:**

		val data = CroppingData()
		data.image = image


1. **Crop the image:**

 		val acuantImage : Image = controller.crop(options,data);
 		


1. **Image class:**

		public class Image {
    			public Bitmap image;
    			public CardType detectedCardType;
    			public boolean hasImageMetrics;
    			public boolean isSharp;
    			public boolean hasGlare;
    			public int sharpnessGrade;
    			public int glareGrade;
    			public Error error;
		}
		
	*Note:*	A sharpness grade of 50 and above is defined as a sharp image. A glare grade of 50 or higher indicates that there is no glare present in the image.

## Facial recognition ##

The following sample describes how to create a live face detector and camera source.


1. **Create a live face detector and a CameraSource:**

		// The first parameter is the activity and the second one is the LiveFaceListener
		val liveFaceDetector = controller.createLiveFaceDetector(context, this)
		
		// Pass the detector to the CameraSourc
		val mCameraSource = CameraSource.Builder(context, liveFaceDetector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedPreviewSize(320, 240)
                .setRequestedFps(60.0f)
                .setAutoFocusEnabled(true)
                .build()
   	 	
1. **LiveFaceListener Interface**

	This interface has the following method
	
		override fun liveFaceDetailsCaptured(liveFaceDetails: LiveFaceDetails) {
        if (liveFaceDetails.error == null) {
            if(liveFaceDetails.isLiveFace){
                // A live face is detected the face image can be accessed as liveFaceDetails.image
            }
        	} else {
        	
        	}
    	}	
		
		
## Process captured images##

Use a Web Service call to process the captured images. 


1. **Set the processing options:**
        
        val cardAttributes = CardAtributes()
        // If it is ID2 type then set the cardWidth correctly
        //cardAttributes.cardType = CardType.ID2
        //cardAttributes.cardWidth = 4.13f
        cardAttributes.cardType = cardType
        
        val idProcessingOptions = IdOptions()
        idProcessingOptions.cardAttributes = cardAttributes
        
*Note:* By default, the processing mode is set to the mode enabled in the subscription. However, if a user only requires data capture, then they can limit the processing mode by setting the following option:

		imageProcessingOptions.processingMode = ProcessingMode.DataCapture

1. **Set the processing data:**
        
        val idProcessingData = IdData()
        imageProcessingData.frontImage = backImage // Bitmap
        imageProcessingData.backImage = frontImage // Bitmap
        imageProcessingData.barcodeString = capturedBarcodeString // String
        
1. **Process the ID**

        controller.processId(idProcessingData,idProcessingOptions,object : ImageProcessingListener{
            override fun imageProcessingFinished(result: ImageProcessingResult?) {
            		// Handle the response
            }

        })
        
            
## Process a Facial Match ##



1. **Set Facial Data:**

		val facialMatchData = FacialMatchData()
		facialMatchData.faceImageOne = capturedFaceImage
		facialMatchData.faceImageTwo = capturedSelfieImage
                    


1. **Process Facial Match:**

 			controller.processFacialMatch(facialMatchData,object : FacialMatchListener{
 				override fun facialMatchFinished(result: FacialMatchResult?) {
                  // Handle response here
             	}
            })


## Read the e-Passport chip ##


1. **Initialize the Android NFC Adapter:**

		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
1. **Ensure that the permission is provided in runtime for API level 23 and above.** 


1. **Use the SDK API to listen to NFC tags available in an e-Passport:**
		
		controller.listenNFC(activity, nfcAdapter, listener)
		
1. **If an NFC Tag is discovered, then the control will return to the method of the Activity that was previously overridden:**

		@Override
    	protected void onNewIntent(Intent intent)
    	{
        	super.onNewIntent(intent);
        	
        	// Read the information from the tag as below
        	controller.readNFCTag(intent, docNumber, dateOfBirth, dateOfExpiry)
		}
       
1. **Implement the Listener methods for success or failure:**
 		
 		override fun tagReadSucceeded(cardDetails: NFCData?, image: Bitmap?, sign_image: Bitmap?) {
 			// Handle Response
 		}

    	override fun tagReadFailed(message: String?) {
    		// Handle error
        
    	}