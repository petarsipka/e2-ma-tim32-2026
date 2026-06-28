package com.example.slagalica.ui.lobby;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.model.Match;
import com.example.slagalica.ui.koznazna.KoZnaZnaActivity;
import com.example.slagalica.util.QrGenerator;
import com.example.slagalica.util.SystemBars;
import com.google.firebase.auth.FirebaseAuth;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * Pairs two devices into a match: the host creates a match and shows its QR/code,
 * the guest scans the QR (or types the code) to join. When both players are in the
 * node, both devices launch {@link KoZnaZnaActivity} for the same match.
 */
public class LobbyActivity extends AppCompatActivity {

    public static final String EXTRA_IS_HOST = "is_host";

    private final MatchRepository repo = new MatchRepository();

    private View llActions;
    private View llHost;
    private ImageView ivQr;
    private TextView tvCode;
    private TextView tvStatus;
    private EditText etCode;

    private String matchCode;
    private boolean isCreator;
    private boolean launched;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    joinByCode(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        SystemBars.apply(this);

        llActions = findViewById(R.id.llActions);
        llHost = findViewById(R.id.llHost);
        ivQr = findViewById(R.id.ivQr);
        tvCode = findViewById(R.id.tvCode);
        tvStatus = findViewById(R.id.tvStatus);
        etCode = findViewById(R.id.etCode);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCreate).setOnClickListener(v -> withAuth(this::createMatch));
        findViewById(R.id.btnScan).setOnClickListener(v -> withAuth(this::startScan));
        findViewById(R.id.btnJoinCode).setOnClickListener(v -> {
            String code = etCode.getText().toString().trim().toUpperCase();
            if (code.isEmpty()) {
                toast("Unesi kod partije");
                return;
            }
            withAuth(() -> joinByCode(code));
        });
    }

    /** Run {@code action} once an anonymous Firebase session exists. */
    private void withAuth(Runnable action) {
        if (repo.currentUid() != null) {
            action.run();
            return;
        }
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> action.run())
                .addOnFailureListener(e -> toast("Greška pri povezivanju sa serverom"));
    }

    private void createMatch() {
        isCreator = true;
        matchCode = repo.createMatch("Domaćin");
        showHostPanel(matchCode);
        repo.listen(matchCode, this::onMatchUpdate);
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Započni partiju");
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        scanLauncher.launch(options);
    }

    private void joinByCode(String code) {
        matchCode = code;
        tvStatus.setText("Pridruživanje partiji " + code + "…");
        repo.joinMatch(code, "Gost", () -> toast("Partija " + code + " ne postoji"));
        repo.listen(code, this::onMatchUpdate);
    }

    private void onMatchUpdate(Match match) {
        if (!launched && match.playerCount() >= 2) {
            launchGame();
        }
    }

    private void launchGame() {
        launched = true;
        repo.detach();
        Intent intent = new Intent(this, KoZnaZnaActivity.class);
        intent.putExtra(KoZnaZnaActivity.EXTRA_MATCH_CODE, matchCode);
        intent.putExtra(EXTRA_IS_HOST, isCreator);
        startActivity(intent);
        finish();
    }

    private void showHostPanel(String code) {
        llActions.setVisibility(View.GONE);
        llHost.setVisibility(View.VISIBLE);
        tvStatus.setText("Pokaži ovaj QR protivniku da skenira.");
        tvCode.setText(code);
        Bitmap qr = QrGenerator.encode(code, 512);
        if (qr != null) ivQr.setImageBitmap(qr);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repo.detach();
    }
}
