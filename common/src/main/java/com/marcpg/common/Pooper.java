package com.marcpg.common;

import com.marcpg.common.logger.Logger;
import com.marcpg.common.util.AsyncScheduler;
import com.marcpg.common.util.UpdateChecker;

import java.nio.file.Path;

public class Pooper {
    // ++++++++++ CONSTANTS ++++++++++
    public static final String VERSION = "1.1.0";
    public static final int BUILD = 3;
    public static final UpdateChecker.Version CURRENT_VERSION = new UpdateChecker.Version(5, VERSION + "+build." + BUILD, "ERROR");
    public static final int METRICS_ID = 21102;


    // ++++++++++ PLUGIN INSTANCE ++++++++++
    public static Platform PLATFORM = Platform.UNKNOWN;
    public static Logger<?> LOG;
    public static Path DATA_DIR;
    public static AsyncScheduler SCHEDULER;
}
