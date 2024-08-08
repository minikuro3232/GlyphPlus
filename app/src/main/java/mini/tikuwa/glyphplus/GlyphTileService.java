package mini.tikuwa.glyphplus;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class GlyphTileService extends TileService {
    public static TileService tiles;
    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTileState();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isActive = prefs.getBoolean("tile_active", false);

        tiles = this;

        SharedPreferences.Editor editor = prefs.edit();
        if (isActive) {
            if (BackgroundService.isRunning()) {
                BackgroundService.stop();
            }
            editor.putBoolean("tile_active", false);
        } else {
            if (!BackgroundService.isRunning()) {
                BackgroundService.start();
            }
            editor.putBoolean("tile_active", true);
        }
        editor.apply();

        GlyphPlus.retrieveInfo();
        updateTileState();
    }

    private void updateTileState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isActive = prefs.getBoolean("tile_active", false);

        Tile tile = getQsTile();
        tile.setState(isActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
