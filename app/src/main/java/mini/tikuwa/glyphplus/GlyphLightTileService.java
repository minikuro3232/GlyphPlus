package mini.tikuwa.glyphplus;

import static mini.tikuwa.glyphplus.GlyphTileService.tiles;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class GlyphLightTileService extends TileService {
    private Context context;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (tiles != null && context != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(tiles);
                boolean isActive = prefs.getBoolean("tile_active", false);

                SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(context);
                boolean isActive2 = prefs2.getBoolean("light_tile_active", false);

                Tile tile = getQsTile();
                tile.setState(isActive ? isActive2 ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE : Tile.STATE_UNAVAILABLE);
                tile.updateTile();
            }

            handler.postDelayed(this, 250);
        }
    };

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTileState();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        handler.postDelayed(runnable, 250);
        context = this;
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
        boolean isActive = prefs.getBoolean("light_tile_active", false);

        SharedPreferences.Editor editor = prefs.edit();
        if (isActive) {
            editor.putBoolean("light_tile_active", false);
            BackgroundService.isLight = false;
        } else {
            editor.putBoolean("light_tile_active", true);
            BackgroundService.isLight = true;
        }
        editor.apply();

        updateTileState();
    }

    private void updateTileState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isActive = prefs.getBoolean("light_tile_active", false);

        Tile tile = getQsTile();
        tile.setState(isActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
