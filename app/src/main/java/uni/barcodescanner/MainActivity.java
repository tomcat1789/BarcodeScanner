package uni.barcodescanner;

//Libaries importieren

import android.Manifest;
import android.app.Activity; //
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//Libaries für Mobile Vision
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

//Libaries für den DOM parser
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;


public class MainActivity extends Activity {

    //Variablen  für BarcodeScanner, Kamera und Views
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private SurfaceView cameraView;
    private TextView barcodeInfo;
    private ListView listItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

// Zuweisungen der Views zu den entsprechenden ID's
        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        barcodeInfo = (TextView) findViewById(R.id.code_info);

// Neuer Barcode Detector
        barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.EAN_13 | Barcode.EAN_8)
                        .build();

// Zugriff auf Kamera, Einstellung der Auflösung und Aktivierung des Autofokus
        cameraSource = new CameraSource
                .Builder(this, barcodeDetector).setAutoFocusEnabled(true)
                .setRequestedPreviewSize(640, 480)
                .build();
// Kamera Methoden
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {

// Kamera starten. Anmerkung: Zeile wird als Fehler makiert, der Code lässt sich aber problemlos kompilieren und ausführen. Mit Permissionscheck wurde ein Fehler angezeigt und der Code lies sich nicht mehr kompilieren
                    cameraSource.start(cameraView.getHolder());




                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

// Zuweisen eines Prozessors
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {

            @Override
            public void release() {
            }

// Verarbeitung der Scan- Resultate
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0) {
                    final String s = barcodes.valueAt(0).displayValue.toString();

                    barcodeInfo.post(new Runnable() {
                        public void run() {
                            result(s);
                            barcodeInfo.setText(
                                    barcodes.valueAt(0).displayValue
                            );
                        }
                    });
                }
            }
        });

    }
// Beenden der Anwendung
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.release();
        barcodeDetector.release();
    }

//===============================================================================================

 // Methode zum Anzeigen der Daten und zur Suche im Web
boolean isActive = false; // Zur Prüfung, ob PopUp bereits geöffnet
public void result (String s) {

    final String ean = s;

    //Anzeigen der Daten vom XML Parser getProductInfo() wenn Daten vorhande
    if(getProductInfo(ean) != null) { //Prüft ob Daten vorhanden
        listItems = (ListView) findViewById(R.id.data);
        listItems.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, getProductInfo(ean)));

    } else if ( getProductInfo(ean) == null && isActive == false) { // Wenn keine Daten vorhanden
    isActive = true;

//Erstellung des PopUp
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle("EAN: " + ean + ". " + "\n Keine Daten gefunden.")
                .setMessage("Im Internet nach EAN suchen?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.de/#q=" + ean)); // Öffnen des Browsers und Suche nach EAN bei Positiver Wahl
                        startActivity(browserIntent);
                        isActive = false;
                    }
                })
                .setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { // Negative Wahl

                        isActive = false;
                    }
                })
                .show();
    }


}

//===============================================================================================

//XML Parser
    public ArrayList getProductInfo (String s) {
        String ean = s;
        ArrayList<String> xml = new ArrayList<String>();

        try
        {

            //Öffnen des XML- File
            InputStream is = getResources().openRawResource(R.raw.product);
            //Parsen des XML- Files
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is, null);
            NodeList nodes = doc.getElementsByTagName("e" +ean); //Suche nach EAN im XML- File
            if(nodes != null && nodes.getLength() > 0) { //Prüfen, ob NodeList leer
                xml.clear();
                int len = nodes.getLength();
                for(int i = 0; i < len; ++i) { //Elemente werden in ArrayList geschrieben.
                    Node node = nodes.item(i);
                    xml.add(node.getTextContent());
                }
            }else {xml =null;}



        }
        catch(Throwable t){
            Toast.makeText(this, "Exception :" + t.toString(), Toast.LENGTH_LONG).show();
        }



        return xml;
    }

    //=============================================================================================


}