package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class drawingpage extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "DrawingPage";

    private DrawingView drawView;
    private ImageView undoBtn, redoBtn, exitBtn, eraserBtn, pencilBtn, penBtn, markerBtn, colorPickerBtn, saveBtn;
    private EditText drawingTitleEditText;
    private FrameLayout drawingPadContainer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String currentDrawingId = null;
    private String initialDrawingBase664 = null;
    private boolean originalIsPinned = false;
    private boolean originalIsLocked = false;
    private String originalHashedPin = null;
    private String originalFolderId = null;
    private boolean originalIsDeleted = false;
    private String originalDeletedDate = null;

    private static final int MAX_IMAGE_SIZE_KB = 700;
    private static final int TARGET_MAX_DIMENSION_PX = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawingpage);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        drawingPadContainer = findViewById(R.id.drawing_pad_container);
        if (drawingPadContainer != null) {
            drawView = new DrawingView(this, null);
            drawingPadContainer.addView(drawView);
        } else {
            Log.e(TAG, "drawing_pad_container not found in layout!");
            Toast.makeText(this, "Error: Drawing canvas container not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "User is null on onCreate, redirecting to login page.");
            Intent loginIntent = new Intent(this, loginpage.class);
            startActivity(loginIntent);
            finish();
            return;
        } else {
            Log.d(TAG, "User authenticated. UID: " + user.getUid());
        }

        drawingTitleEditText = findViewById(R.id.drawing_title_edittext);
        undoBtn = findViewById(R.id.undo);
        redoBtn = findViewById(R.id.redo);
        exitBtn = findViewById(R.id.exit);
        eraserBtn = findViewById(R.id.eraser);
        pencilBtn = findViewById(R.id.pencil);
        penBtn = findViewById(R.id.pen);
        markerBtn = findViewById(R.id.marker);
        colorPickerBtn = findViewById(R.id.color_picker_button);
        saveBtn = findViewById(R.id.save_drawing_button);

        undoBtn.setOnClickListener(this);
        redoBtn.setOnClickListener(this);
        eraserBtn.setOnClickListener(this);
        pencilBtn.setOnClickListener(this);
        penBtn.setOnClickListener(this);
        markerBtn.setOnClickListener(this);
        colorPickerBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        exitBtn.setOnClickListener(v -> showSaveConfirmationDialog());

        drawView.setBrushSize(drawView.getMediumBrushSize());
        drawView.setColor(Color.BLACK);
        updateColorDisplay(Color.BLACK);

        Log.d(TAG, "Creating new drawing activity started.");
    }

    private void updateColorDisplay(int color) {
        if (colorPickerBtn.getDrawable() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) colorPickerBtn.getDrawable();
            drawable.setColor(color);
        } else {
            colorPickerBtn.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.undo) {
            drawView.undo();
            Toast.makeText(this, "Undo action performed", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.redo) {
            drawView.redo();
            Toast.makeText(this, "Redo action performed", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.eraser) {
            drawView.setErase(true);
            Toast.makeText(this, "Eraser Activated", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.pencil) {
            drawView.setBrushSize(drawView.getThinBrushSize());
            drawView.setErase(false);
            Toast.makeText(this, "Pencil (Thin brush) selected", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.pen) {
            drawView.setBrushSize(drawView.getMediumBrushSize());
            drawView.setErase(false);
            Toast.makeText(this, "Pen (Medium brush) selected", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.marker) {
            drawView.setBrushSize(drawView.getThickBrushSize());
            drawView.setErase(false);
            Toast.makeText(this, "Marker (Thick brush) selected", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.color_picker_button) {
            showColorPickerDialog();
        } else if (id == R.id.save_drawing_button) {
            saveDrawingToFirestore();
        }
    }

    private void showColorPickerDialog() {
        new ColorPickerDialog.Builder(this)
                .setTitle("Choose Color")
                .setPreferenceName("ColorPickerDrawing")
                .setPositiveButton("Select", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    int selectedColor = envelope.getColor();
                    drawView.setColor(selectedColor);
                    updateColorDisplay(selectedColor);
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(false)
                .setBottomSpace(12)
                .show();
    }

    private void showSaveConfirmationDialog() {
        AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
        saveDialog.setTitle("Save Drawing");
        saveDialog.setMessage("Do you want to save your drawing before exiting?");
        saveDialog.setPositiveButton("Yes", (dialog, which) -> saveDrawingToFirestore());
        saveDialog.setNegativeButton("No", (dialog, which) -> {
            Log.d(TAG, "User chose not to save. Navigating to SecondaryPage.");
            navigateToSecondaryPage();
        });
        saveDialog.setNeutralButton("Cancel", (dialog, which) -> {
            Log.d(TAG, "User cancelled exit.");
            dialog.dismiss();
        });
        saveDialog.show();
    }

    private void saveDrawingToFirestore() {
        Log.d(TAG, "Attempting to save drawing to Firestore...");
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Save failed: User is null.");
            return;
        }
        Log.d(TAG, "User authenticated: " + user.getUid());

        String drawingName = drawingTitleEditText.getText().toString().trim();
        if (drawingName.isEmpty()) {
            drawingName = "Untitled Drawing";
            Log.d(TAG, "Drawing name is empty, setting to: " + drawingName);
        }

        if (drawView.getWidth() == 0 || drawView.getHeight() == 0) {
            Toast.makeText(this, "Drawing surface not ready. Please try again.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Save failed: DrawingView has zero width or height. Retrying after layout.");
            drawView.post(this::saveDrawingToFirestore);
            return;
        }

        Bitmap drawingBitmap = drawView.getDrawingBitmap();
        if (drawingBitmap == null) {
            Toast.makeText(this, "Failed to get drawing bitmap. Nothing to save.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Save failed: getDrawingBitmap returned null.");
            return;
        }

        String base64Image = reduceBitmapAndConvertToBase64(drawingBitmap);
        if (base64Image == null) {
            Toast.makeText(this, "Failed to compress or convert drawing to Base64.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Save failed: Base64 conversion returned null.");
            return;
        }
        Log.d(TAG, "Bitmap converted to Base64. String length: " + base64Image.length() + " (approx. " + (base64Image.length() / 1024) + " KB)");

        saveDrawingDetailsToFirestore(user.getUid(), drawingName, base64Image);
    }

    private String reduceBitmapAndConvertToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        float scale = 1.0f;

        if (originalWidth > TARGET_MAX_DIMENSION_PX || originalHeight > TARGET_MAX_DIMENSION_PX) {
            if (originalWidth > originalHeight) {
                scale = (float) TARGET_MAX_DIMENSION_PX / originalWidth;
            } else {
                scale = (float) TARGET_MAX_DIMENSION_PX / originalHeight;
            }
        }

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 90;
        String base64Image = null;

        for (int i = 0; i < 5; i++) {
            baos.reset();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] byteArray = baos.toByteArray();

            if (byteArray.length / 1024.0 <= MAX_IMAGE_SIZE_KB) {
                base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);
                Log.d(TAG, "Compressed bitmap to " + (byteArray.length / 1024.0) + " KB (quality: " + quality + "), Base64 length: " + base64Image.length());
                break;
            }
            quality -= 20;
            if (quality < 10) quality = 10;
            Log.d(TAG, "Image too large, reducing quality to " + quality);
        }

        if (base64Image == null || baos.toByteArray().length / 1024.0 > MAX_IMAGE_SIZE_KB) {
            baos.reset();
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] pngByteArray = baos.toByteArray();
            if (pngByteArray.length / 1024.0 <= MAX_IMAGE_SIZE_KB) {
                base64Image = Base64.encodeToString(pngByteArray, Base64.DEFAULT);
                Log.d(TAG, "Final attempt with PNG. Compressed to " + (pngByteArray.length / 1024.0) + " KB, Base64 length: " + base64Image.length());
            } else {
                Log.e(TAG, "Bitmap still too large after all compression attempts: " + (pngByteArray.length / 1024.0) + " KB");
                Toast.makeText(this, "Drawing is too large to save. Try a simpler drawing.", Toast.LENGTH_LONG).show();
                return null;
            }
        }

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle();
        }

        return base64Image;
    }

    private void saveDrawingDetailsToFirestore(String userId, String title, String base64Image) {
        Log.d(TAG, "Entering saveDrawingDetailsToFirestore (for new drawing). Title: '" + title + "', Base64 length: '" + base64Image.length() + "'");
        Map<String, Object> drawingNoteData = new HashMap<>();
        drawingNoteData.put("note_title", title);
        drawingNoteData.put("imageUrl", base64Image);
        drawingNoteData.put("timestamp", new Date());
        drawingNoteData.put("type", "drawing");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault());
        drawingNoteData.put("note_date", sdf.format(new Date()));

        drawingNoteData.put("note_content", "");

        drawingNoteData.put("isPinned", originalIsPinned);
        drawingNoteData.put("isLocked", originalIsLocked);
        drawingNoteData.put("hashedPin", originalHashedPin);
        drawingNoteData.put("folder_id", originalFolderId);
        drawingNoteData.put("isDeleted", originalIsDeleted);
        drawingNoteData.put("deleted_date", originalDeletedDate);


        db.collection("users").document(userId).collection("miscellaneous_notes")
                .add(drawingNoteData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Initial Firestore document created for new drawing. ID: " + documentReference.getId());
                    documentReference.update("note_id", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(drawingpage.this, "Drawing saved successfully!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "New drawing details saved and ID updated in Firestore successfully.");
                                navigateToSecondaryPage();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(drawingpage.this, "Error setting drawing ID: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Error setting drawing ID in Firestore (new drawing)", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(drawingpage.this, "Failed to save new drawing details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Firestore add failed (new drawing)", e);
                });
    }

    private void navigateToSecondaryPage() {
        Intent intent = new Intent(drawingpage.this, mainpage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadDrawingAsBackground(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            Log.d(TAG, "No initial drawing Base64 data provided to load as background.");
            return;
        }
        Toast.makeText(this, "Loading original drawing...", Toast.LENGTH_SHORT).show();

        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            if (decodedBitmap != null) {
                Log.d(TAG, "Original drawing bitmap decoded from Base64. Dimensions: " + decodedBitmap.getWidth() + "x" + decodedBitmap.getHeight());
                drawView.setBackgroundImage(decodedBitmap);
                Toast.makeText(drawingpage.this, "Original drawing loaded as background. New strokes will overwrite it on save.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(drawingpage.this, "Failed to decode original drawing image from Base64.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "BitmapFactory failed to decode Base64 string.");
            }
        } catch (IllegalArgumentException e) {
            Toast.makeText(drawingpage.this, "Invalid Base64 data for drawing.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error decoding Base64 image: ", e);
        } catch (Exception e) {
            Toast.makeText(this, "An unexpected error occurred while loading drawing.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unexpected error loading drawing from Base64: ", e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showSaveConfirmationDialog();
    }
}