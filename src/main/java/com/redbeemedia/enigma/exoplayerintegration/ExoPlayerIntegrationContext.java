package com.redbeemedia.enigma.exoplayerintegration;

import android.app.Application;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.offline.DefaultDownloadIndex;
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.offline.WritableDownloadIndex;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.redbeemedia.enigma.core.context.EnigmaRiverContext;
import com.redbeemedia.enigma.core.context.IModuleContextInitialization;

import java.io.File;

public class ExoPlayerIntegrationContext {
    private static volatile InitializedContext initializedContext = null;

    private ExoPlayerIntegrationContext() {} // Disable instantiation

    /**
     * Called by core module through reflection.
     */
    private static synchronized void initialize(IModuleContextInitialization initialization) {
        if(initializedContext == null) {
            initializedContext = new InitializedContext(initialization);
        } else {
            throw new IllegalStateException("ExoPlayerIntegrationContext already initialized.");
        }
    }

    public static DownloadManager getDownloadManager() {
        assertInitialized();
        return initializedContext.downloadManager;
    }

    public static Cache getDownloadCache() {
        assertInitialized();
        return initializedContext.downloadCache;
    }

    public static synchronized void assertInitialized() {
        if(initializedContext == null) {
            // If EnigmaRiverContext is not yet initialized,
            // getVersion() will throw an exception. This
            // indicates that the reason this module is not
            // yet initialized is that the parent module is
            // not initialized.
            String version = EnigmaRiverContext.getVersion();
            throw new IllegalStateException("ExoPlayerIntegrationContext was not initialized from core module. Make sure "+version+" is used for all Enigma River SDK modules.");
        }
    }


    private static class InitializedContext {
        private final DownloadManager downloadManager;
        private final Cache downloadCache;

        public InitializedContext(IModuleContextInitialization initialization) {
            Application application = initialization.getApplication();

            ExoDatabaseProvider databaseProvider = new ExoDatabaseProvider(application);
            WritableDownloadIndex downloadIndex = new DefaultDownloadIndex(databaseProvider);

            File downloadDirectory = application.getFilesDir();

            File downloadContentDirectory = new File(downloadDirectory, "downloads");
            downloadCache =
                    new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), databaseProvider);

            HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSourceFactory("enigma_river_download");

        DownloaderConstructorHelper downloaderConstructorHelper = new DownloaderConstructorHelper(downloadCache, httpDataSourceFactory);
        downloadManager = new DownloadManager(
                                            application,
                                            downloadIndex,
                                            new DefaultDownloaderFactory(downloaderConstructorHelper));

        }
    }
}
