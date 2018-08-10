
package com.acuant.sampleapp.documentcapturecamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.acuant.acuantmobilesdk.DocumentListener;
import com.acuant.acuantmobilesdk.internal.livedocumentprocessing.DocumentDetector;
import com.acuant.acuantmobilesdk.models.CapturedDocument;
import com.acuant.sampleapp.AppInstance;
import com.acuant.sampleapp.CapturedImage;
import com.acuant.sampleapp.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.IOException;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
public final class DocumentCaptureActivity extends AppCompatActivity implements DocumentListener {
    private static final String TAG = "Barcode-reader";
    private int ORIENTATION_PORTRAIT_REVERSE = 4;
    private int ORIENTATION_LANDSCAPE_REVERSE = 3;

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private DocumentCameraSource documentCameraSource;
    private DocumentCameraSourcePreview mPreview;
    private DocumentGraphicOverlay<DocumentGraphic> documentGraphicOverlay;
    private boolean tapped = false;
    private int waitTime  = 2;
    DocumentDetector documentDetector = null;

    private TextView instructionView = null;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        tapped = false;
        waitTime = getIntent().getIntExtra("WAIT",2);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().setTitle("");
        getSupportActionBar().hide();
        RelativeLayout parent = new RelativeLayout(this);
        parent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        parent.setKeepScreenOn(true);



        setContentView(parent);
        mPreview = new DocumentCameraSourcePreview(this, null);
        parent.addView(mPreview);

        documentGraphicOverlay = new DocumentGraphicOverlay<>(this, null);
        parent.addView(documentGraphicOverlay);
        if (documentGraphicOverlay != null) {
            documentGraphicOverlay.setVisibility(View.GONE);
        }

        // UI Customization
        RelativeLayout.LayoutParams tvlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        tvlp.addRule(RelativeLayout.CENTER_IN_PARENT);
        if(instructionView == null) {
            instructionView = new TextView(this);
            instructionView.setTextSize(33f);
            instructionView.setTextColor(Color.WHITE);
            instructionView.setPadding(60, 15, 60, 15);
            instructionView.setGravity(Gravity.CENTER);
            instructionView.setRotation(90.0f);
            instructionView.setBackgroundColor(Color.RED);
            instructionView.setLayoutParams(tvlp);
            parent.addView(instructionView, tvlp);
        }
        instructionView.setText("ALIGN AND TAP");

        RelativeLayout.LayoutParams vfvp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        BracketsView bracketsView  = new BracketsView(this, null);
        bracketsView.setLayoutParams(vfvp);
        parent.addView(bracketsView, vfvp);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(true, false);
        } else {
            requestCameraPermission();
        }
        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (tapped) {
                    return false;
                }
                tapped = true;
                instructionView.setText("HOLD STEADY");
                documentDetector.startDetectingPdf417(waitTime);
                if (documentGraphicOverlay != null) {
                    documentGraphicOverlay.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });

    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();
        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        documentDetector = AppInstance.Companion.getInstance().getController().createDocumentDetector(context,this);
        DocumentCameraSource.Builder builder = new DocumentCameraSource.Builder(getApplicationContext(), documentDetector)
                .setFacing(DocumentCameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(60.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        documentCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        tapped = false;
        OrientationEventListener mOrientationEventListener = new OrientationEventListener(this) {
            int lastOrientation = 0;
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation < 0) {
                    return ; // Flip screen, Not take account
                }
                int curOrientation;
                if (orientation <= 45) {
                    curOrientation = ORIENTATION_PORTRAIT;
                } else if (orientation <= 135) {
                    curOrientation = ORIENTATION_LANDSCAPE_REVERSE;
                } else if (orientation <= 225) {
                    curOrientation = ORIENTATION_PORTRAIT_REVERSE;
                } else if (orientation <= 315) {
                    curOrientation = ORIENTATION_LANDSCAPE;
                } else {
                    curOrientation = ORIENTATION_PORTRAIT;
                }
                if (curOrientation != lastOrientation) {
                    onChanged(lastOrientation, curOrientation);
                    lastOrientation = curOrientation;
                }

            }
        };

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            createCameraSource(true, false);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (documentCameraSource != null) {
            try {
                mPreview.start(documentCameraSource, documentGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                documentCameraSource.release();
                documentCameraSource = null;
            }
        }
    }

    @Override
    public void onDocumentDetected(final CapturedDocument capturedDocument) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent result = new Intent();
                CapturedImage.Companion.setImage(capturedDocument.documentImage);
                CapturedImage.Companion.setBarcodeString(capturedDocument.barcodeString);
                setResult(Constants.Companion.getREQUEST_CAMERA_PHOTO(), result);
                finish();
            }
        });

    }

    protected void onChanged(final int lastOrientation, final int curOrientation) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (curOrientation == ORIENTATION_LANDSCAPE_REVERSE) {
                    rotateView(instructionView, 0, 270);


                } else if (curOrientation == ORIENTATION_LANDSCAPE) {
                    rotateView(instructionView, 360, 90);


                }
            }
        });

    }

    private void rotateView(View view, float startDeg, float endDeg) {
        if (view != null) {
            view.setRotation(startDeg);
            view.animate().rotation(endDeg).start();
        }
    }
}
